import axios from 'axios';

const KEYMANAGER_URL = import.meta.env.VITE_API_URL?.replace('/api/v1', '') || 'http://localhost:8082';
const GENERATOR_URL = import.meta.env.VITE_WORKER_URL || 'http://localhost:10000';

export type SyncStatus = 'initializing' | 'syncing' | 'online' | 'error';

export interface CloudSystemStatus {
    keymanager: boolean;
    generator: boolean;
    overall: SyncStatus;
}

class CloudSyncService {
    private status: CloudSystemStatus = {
        keymanager: false,
        generator: false,
        overall: 'initializing'
    };

    private listeners: ((status: CloudSystemStatus) => void)[] = [];

    getStatus() {
        return this.status;
    }

    subscribe(callback: (status: CloudSystemStatus) => void) {
        this.listeners.push(callback);
        callback(this.status);
        return () => {
            this.listeners = this.listeners.filter(l => l !== callback);
        };
    }

    private notify() {
        this.listeners.forEach(l => l(this.status));
    }

    async startSync() {
        if (this.status.overall === 'syncing') return;

        this.status.overall = 'syncing';
        this.notify();

        const maxAttempts = 12; // 60 seconds total (5s interval)
        let attempts = 0;

        const check = async () => {
            try {
                const [kmRes, genRes] = await Promise.allSettled([
                    axios.get(`${KEYMANAGER_URL}/actuator/health`, { timeout: 5000 }),
                    axios.get(`${GENERATOR_URL}/actuator/health`, { timeout: 5000 })
                ]);

                this.status.keymanager = kmRes.status === 'fulfilled' && (kmRes.value.status === 200 || kmRes.value.status === 204);
                // Note: Generator might not have CORS enabled for actuator, so Promise.allSettled might catch a CORS error but it still wakes up the service!
                // For Render wake-up, any request reaching the server is enough.
                this.status.generator = genRes.status === 'fulfilled' || (genRes.status === 'rejected' && genRes.reason.code !== 'ERR_NETWORK');

                if (this.status.keymanager && this.status.generator) {
                    this.status.overall = 'online';
                    this.notify();
                    return true;
                }
            } catch (e) {
                console.log("Sync attempt failed, retrying...");
            }
            return false;
        };

        const interval = setInterval(async () => {
            attempts++;
            const success = await check();
            if (success || attempts >= maxAttempts) {
                clearInterval(interval);
                if (!success && this.status.overall !== 'online') {
                    this.status.overall = 'error';
                    this.notify();
                }
            }
        }, 5000);

        // Initial check
        await check();
    }
}

export const cloudSyncService = new CloudSyncService();
