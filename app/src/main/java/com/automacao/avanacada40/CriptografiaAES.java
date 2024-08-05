package com.automacao.avanacada40;

import android.util.Log;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CriptografiaAES extends Thread {

    private List<String> textos;
    private String texto;
    private boolean criptografar;
    private List<String> resultadoDescriptografia;
    private String resultadoCriptografia;

    public CriptografiaAES(List<String> textos, boolean criptografar) {
        this.textos = textos;
        this.criptografar = criptografar;
        this.resultadoDescriptografia = new ArrayList<>();
    }
    public CriptografiaAES(String texto, boolean criptografar) {
        this.texto = texto;
        this.criptografar = criptografar;

    }
    private static final String CHAVE_SECRETA = "ChaveSecreta1234"; // A chave deve ter 16, 24 ou 32 bytes

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";


    @Override
    public void run() {

        try {
            if (criptografar) {

                resultadoCriptografia = criptografar(texto);

            } else {
                int i = 0;
                for (String texto : textos) {
                    resultadoDescriptografia.add(i, descriptografar(texto));
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static String criptografar(String texto) throws Exception {
        SecretKeySpec chave = new SecretKeySpec(CHAVE_SECRETA.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, chave);
        byte[] textoCriptografado = cipher.doFinal(texto.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(textoCriptografado);
    }

    private static String descriptografar(String textoCriptografado) throws Exception {
        if (textoCriptografado == null) {
            return null;
        }
        SecretKeySpec chave = new SecretKeySpec(CHAVE_SECRETA.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, chave);
        byte[] textoBytes = Base64.getDecoder().decode(textoCriptografado);
        byte[] textoDescriptografado = cipher.doFinal(textoBytes);
        return new String(textoDescriptografado, "UTF-8");
    }

    public List<String> obterResultadoDescriptografia() {
        return resultadoDescriptografia;
    }
    public String obterResultadoCriptografia() {
        return resultadoCriptografia;
    }

}