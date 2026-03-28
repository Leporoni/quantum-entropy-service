import React, { useEffect, useState } from 'react';
import { cloudSyncService, type CloudSystemStatus } from '../services/cloudSync';
import { ShieldCheck, ShieldAlert, Cpu } from 'lucide-react';
import './CloudStatus.css';

const CloudStatus: React.FC = () => {
    const [status, setStatus] = useState<CloudSystemStatus>(cloudSyncService.getStatus());

    useEffect(() => {
        return cloudSyncService.subscribe(setStatus);
    }, []);

    const getStatusConfig = () => {
        switch (status.overall) {
            case 'initializing':
            case 'syncing':
                return {
                    icon: <Cpu className="spin-slow" size={14} />,
                    text: 'QUANTUM SYNC: INITIALIZING',
                    className: 'status-syncing'
                };
            case 'online':
                return {
                    icon: <ShieldCheck size={14} />,
                    text: 'QUANTUM NETWORK: ONLINE',
                    className: 'status-online'
                };
            case 'error':
                return {
                    icon: <ShieldAlert size={14} />,
                    text: 'QUANTUM SYNC: PARTIAL',
                    className: 'status-error'
                };
        }
    };

    const config = getStatusConfig();

    return (
        <div className={`cloud-status-badge ${config.className}`}>
            {config.icon}
            <span>{config.text}</span>
        </div>
    );
};

export default CloudStatus;
