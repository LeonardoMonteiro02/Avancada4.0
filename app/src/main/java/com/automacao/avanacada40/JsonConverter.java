package com.automacao.avanacada40;



import android.util.Log;

import com.example.biblioteca.Region;
import com.example.biblioteca.RestrictedRegion;
import com.example.biblioteca.SubRegion;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonConverter extends Thread {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private boolean convertToJson; // Booleano para indicar se deve converter para JSON ou para objeto
    private String encryptedData;
    private String jsonencryptedData;
    private  Object obeject;
    private List<Region> region = new ArrayList<>();



    // Construtor que recebe o Dado para descriptograr
    public JsonConverter(String encryptedData, boolean convertToJson) {
        this.convertToJson = convertToJson;
        this.encryptedData = encryptedData;
    }

    // Construtor que recebe um obejeto para Criptograr
    public JsonConverter(Object obeject, boolean convertToJson) {
        this.convertToJson = convertToJson;
        this.obeject = obeject;
    }


    // Sobrescrevendo o método run() da classe Thread
    @Override
    public void run() {

        // Verifica se deve converter para JSON ou para objeto
        if (convertToJson) {
            // Se convertToJson for true, chama o método para converter objeto para JSON
            jsonencryptedData = objectToJsonEncrypted(obeject); // Substitua 'new Object()' pelo objeto que deseja converter

        } else {
            // Se convertToJson for false, chama o método para converter JSON para objeto
            jsonToObjectDecrypted(encryptedData);

        }

    }

    private static String objectToJsonEncrypted(Object obj) {
        try {
            // Convertendo o objeto para JSON
            String json = objectMapper.writeValueAsString(obj);

            // Convertendo o JSON para um objeto JSON
            JSONObject jsonObject = new JSONObject(json);

            // Iterando sobre as chaves do objeto JSON
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                // Verificando se o valor associado à chave é um objeto JSON
                if (jsonObject.get(key) instanceof JSONObject) {
                    // Se for um objeto JSON, iteramos sobre as suas chaves
                    JSONObject innerObject = jsonObject.getJSONObject(key);
                    Iterator<String> innerKeys = innerObject.keys();
                    while (innerKeys.hasNext()) {
                        String innerKey = innerKeys.next();
                        CriptografiaAES criptografar = new CriptografiaAES(innerObject.get(innerKey).toString(), true);
                        criptografar.start();
                        criptografar.join();
                        // Criptografando o valor associado à chave do objeto interno
                        String encryptedValue = criptografar.obterResultadoCriptografia();
                        // Substituindo o valor original pelo valor criptografado
                        innerObject.put(innerKey, encryptedValue);
                    }
                } else {
                    // Criptografando o valor associado à chave
                    CriptografiaAES criptografar = new CriptografiaAES(jsonObject.get(key).toString(), true);
                    criptografar.start();
                    criptografar.join();
                    String encryptedValue = criptografar.obterResultadoCriptografia();
                    // Substituindo o valor original pelo valor criptografado
                    jsonObject.put(key, encryptedValue);
                }
            }

            // Convertendo o objeto JSON modificado de volta para uma string JSON
            return jsonObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Método para converter JSON criptografado de volta para um objeto



    public List<Region> obterResultado() {
        return region;
    }
    public String datacriptografado(){
        return jsonencryptedData;
    }

    private void jsonToObjectDecrypted(String stringEncrypted){
        try {
            JSONObject jsonEncrypted = new JSONObject(stringEncrypted);
            if (jsonEncrypted.has("restricted") && jsonEncrypted.has("mainRegion")) {

                // Adicionar a região restrita reconstruída à lista de regiões
                region.add(reconstruirRestrictedRegion(jsonEncrypted));

            }

            else if (jsonEncrypted.has("mainRegion")) {

                // Adicionar a sub-região reconstruída à lista de regiões
                region.add(reconstruirSubRegion(jsonEncrypted));

            }

            // Verificar se é uma Região simples
            else if (jsonEncrypted.has("latitude") && jsonEncrypted.has("longitude") &&
                    jsonEncrypted.has("name") && jsonEncrypted.has("timestamp") &&
                    jsonEncrypted.has("user")) {
                region.add(reconstruirRegiaoPrincipal(jsonEncrypted));

            }



            } catch (JSONException e) {
            throw new RuntimeException(e);
        }


    }
    private Region reconstruirRegiaoPrincipal( JSONObject encryptedData) {
        List<String> lista = new ArrayList<>();
        try {
            lista.add(encryptedData.getString("latitude"));
            lista.add(encryptedData.getString("longitude"));
            lista.add(encryptedData.getString("name"));
            lista.add(encryptedData.getString("timestamp"));
            lista.add(encryptedData.getString("user"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        CriptografiaAES descriptografar = new CriptografiaAES(lista, false);
        descriptografar.start();

        try {
            descriptografar.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        lista = descriptografar.obterResultadoDescriptografia();


        // Descriptografar os valores dos atributos
        double latitude = Double.parseDouble(lista.get(0));
        double longitude = Double.parseDouble(lista.get(1));
        String name = lista.get(2);
        long timestamp = Long.parseLong(lista.get(3));
        int user = Integer.parseInt(lista.get(4));

        // Construir o objeto Region com os valores descriptografados
        Region region = new Region(name, latitude, longitude, timestamp, user);


        return region;
    }

    private SubRegion reconstruirSubRegion( JSONObject encryptedData) {
        Region region = reconstruirRegiaoPrincipal(encryptedData);
        JSONObject encryptedMainRegion = null;
        try {
            encryptedMainRegion = encryptedData.getJSONObject("mainRegion");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Region mainRegion= reconstruirRegiaoPrincipal(encryptedMainRegion);


        SubRegion subRegion = new SubRegion(region.getName(),region.getLatitude(), region.getLongitude(),region.getuser(),region.getTimestamp(),mainRegion);


        return subRegion;
    }

    private RestrictedRegion reconstruirRestrictedRegion( JSONObject encryptedData) {
        SubRegion subRegion = reconstruirSubRegion(encryptedData);
        List<String> lista = new ArrayList<>();
        try {
            lista.add(encryptedData.getString("restricted"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        CriptografiaAES descriptografar = new CriptografiaAES(lista, false);
        descriptografar.start();

        try {
            descriptografar.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        boolean restricted = Boolean.parseBoolean(descriptografar.obterResultadoDescriptografia().get(0));

        RestrictedRegion restrictedRegion = new RestrictedRegion(subRegion.getName(), subRegion.getLatitude(), subRegion.getLongitude(), subRegion.getuser(), subRegion.getTimestamp(),
                restricted, subRegion.getMainRegion());
        return restrictedRegion;

    }


}
