import forge from 'node-forge';

/**
 * Descriptografa a chave privada recebida do backend usando o protocolo de Key Wrapping.
 *
 * <p>O backend Java criptografa a chave privada com uma chave de transporte temporária (AES)
 * antes de enviá-la. Esta função reverte o processo no lado do cliente para garantir
 * que a chave privada em texto plano nunca trafegue pela rede.
 *
 * @param encryptedPrivateKeyBase64 A chave privada criptografada, codificada em Base64.
 * @param transportKeyBase64 A chave de transporte (AES), codificada em Base64.
 * @returns A chave privada original em formato PEM.
 * @throws {Error} Se a descriptografia falhar devido a uma chave incorreta, padding inválido ou outro erro.
 */
export const decryptPrivateKey = (encryptedPrivateKeyBase64: string, transportKeyBase64: string): string => {
  try {
    // 1. Decodificar as strings Base64 para obter os bytes brutos.
    const encryptedBytes = forge.util.decode64(encryptedPrivateKeyBase64);
    const keyBytes = forge.util.decode64(transportKeyBase64);

    // 2. Configurar e executar a descriptografia AES-ECB.
    // O modo "AES-ECB" é usado para ser compatível com o padrão `Cipher.getInstance("AES")` do Java.
    const decipher = forge.cipher.createDecipher('AES-ECB', keyBytes);
    decipher.start();
    decipher.update(forge.util.createBuffer(encryptedBytes));
    const result = decipher.finish(); // 'result' é true em sucesso, false em falha de padding

    if (!result) {
      throw new Error("Falha na descriptografia: Padding incorreto ou chave inválida.");
    }

    // 3. O resultado da descriptografia (output) é a string da chave privada original.
    const decryptedString = decipher.output.toString();

    // 4. Formata a string Base64 crua para o formato PEM padrão.
    return formatAsPem(decryptedString, "PRIVATE KEY");

  } catch (error) {
    console.error("Erro na descriptografia:", error);
    throw new Error("Não foi possível descriptografar a chave privada.");
  }
};

/**
 * Formata uma string de corpo Base64 em um bloco PEM padrão.
 * Adiciona o cabeçalho, rodapé e as quebras de linha a cada 64 caracteres.
 *
 * @param base64Body A string Base64 pura (corpo da chave).
 * @param type O tipo de chave para o cabeçalho/rodapé (ex: "PRIVATE KEY", "PUBLIC KEY").
 * @returns A chave formatada como um bloco de texto PEM.
 */
export const formatAsPem = (base64Body: string, type: string): string => {
  const header = `-----BEGIN ${type}-----`;
  const footer = `-----END ${type}-----`;

  // Quebra o corpo da string em linhas de 64 caracteres.
  const body = base64Body.match(/.{1,64}/g)?.join('\n') || base64Body;

  return `${header}\n${body}\n${footer}`;
};

/**
 * Formata uma chave pública (já em Base64) para o formato PEM.
 *
 * @param publicKeyBase64 A chave pública codificada em Base64.
 * @returns A chave pública formatada em PEM.
 */
export const formatPublicKey = (publicKeyBase64: string): string => {
    return formatAsPem(publicKeyBase64, "PUBLIC KEY");
};
