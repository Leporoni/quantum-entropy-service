import forge from 'node-forge';

/**
 * Descriptografa a chave privada recebida do backend.
 * O Backend Java usa AES (provavelmente AES/ECB/PKCS5Padding) com uma chave de transporte derivada de entropia quântica.
 * 
 * @param encryptedPrivateKeyBase64 A chave privada criptografada em Base64
 * @param transportKeyBase64 A chave de transporte (AES-256) em Base64
 * @returns A chave privada original em formato PEM
 */
export const decryptPrivateKey = (encryptedPrivateKeyBase64: string, transportKeyBase64: string): string => {
  try {
    // 1. Decodificar Base64 para Bytes
    const encryptedBytes = forge.util.decode64(encryptedPrivateKeyBase64);
    const keyBytes = forge.util.decode64(transportKeyBase64);

    // 2. Configurar a descriptografia AES-ECB (Default do Java "AES")
    const decipher = forge.cipher.createDecipher('AES-ECB', keyBytes);
    decipher.start();
    decipher.update(forge.util.createBuffer(encryptedBytes));
    const result = decipher.finish();

    if (!result) {
      throw new Error("Falha na descriptografia: Padding incorreto ou chave inválida.");
    }

    // 3. O resultado é a String da Chave Privada em Base64 (formato que o Java salvou antes de encriptar)
    // O Java faz: privateKeyBase64.getBytes() -> AES Encrypt -> Base64
    // Então aqui temos: AES Decrypt -> privateKeyBase64
    
    // Convertendo o buffer de saída para string
    const decryptedString = decipher.output.toString();

    // Como o Java retornou `Base64.encode(pair.getPrivate().getEncoded())` antes de criptografar,
    // a string decriptada 'decryptedString' já é o corpo da chave em Base64 puro (sem headers).
    // Vamos formatá-la como PEM para ficar bonito.
    
    return formatAsPem(decryptedString, "PRIVATE KEY");

  } catch (error) {
    console.error("Erro na descriptografia:", error);
    throw new Error("Não foi possível descriptografar a chave privada.");
  }
};

/**
 * Formata uma string Base64 crua em formato PEM (com headers e quebras de linha).
 */
export const formatAsPem = (base64Body: string, type: string): string => {
  const header = `-----BEGIN ${type}-----`;
  const footer = `-----END ${type}-----`;
  
  // Quebrar em linhas de 64 caracteres
  const body = base64Body.match(/.{1,64}/g)?.join('\n') || base64Body;
  
  return `${header}\n${body}\n${footer}`;
};

/**
 * Formata a chave pública (que já vem em Base64 do Java) para PEM
 */
export const formatPublicKey = (publicKeyBase64: string): string => {
    return formatAsPem(publicKeyBase64, "PUBLIC KEY");
};
