
package com.automacao.avanacada40;


import android.util.Log;

import androidx.annotation.NonNull;


import com.example.biblioteca.Region;
import com.example.biblioteca.RestrictedRegion;
import com.example.biblioteca.SubRegion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ConsultDatabase extends Thread {
    private List<Region> regions;
    private String newName;
    private double newlatitude;
    private double newlongitude;
    private Semaphore semaphore;
    private long startTime;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();


    public ConsultDatabase(List<Region> regions, String locationName, double latitude, double longitude, Semaphore semaphore) {
        this.regions = regions;
        this.newName = locationName;
        this.newlatitude = latitude;
        this.newlongitude = longitude;
        this.semaphore = semaphore;

    }

    @Override
    public void run() {
        startTime = System.nanoTime();
        Log.d("Consulta Banco de Dados", "Thread Inicializada");
        Log.d("Consulta Banco de Dados", "Nova localização " + newName);
        consultarBanco();

    }

    private void consultarBanco() {
        DatabaseReference regioesRef = databaseReference.child("regioes");
        List<Region> regionsFromDatabase = new ArrayList<>();
        regioesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long startTime = System.nanoTime();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    try {
                        JsonConverter jsonConverter = new JsonConverter(childSnapshot.getValue(String.class),false);
                        jsonConverter.start();
                        try {
                            jsonConverter.join();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        regionsFromDatabase.add(jsonConverter.obterResultado().get(0));


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Registra o tempo de término da conversão + criptografia
                long endTime = System.nanoTime();
                // Calcula o tempo decorrido em nanossegundos
                long elapsedTime = endTime - startTime;
                // Use o tempo decorrido conforme necessário, como registrá-lo em logs ou realizar outras ações
                Log.d("Consulta Lista", "Tempo decorrido para conversão + criptografia " + elapsedTime + " nanossegundos");


                processarRegioes(regionsFromDatabase);

                // Registra o tempo de término da verificação no banco
                long endTime2 = System.nanoTime();
                // Calcula o tempo decorrido em nanossegundos
                long elapsedTime2 = endTime2 - startTime;
                // Use o tempo decorrido conforme necessário, como registrá-lo em logs ou realizar outras ações
                Log.d("Consulta Lista", "Tempo decorrido para verificar o banco " + elapsedTime2 + " nanossegundos");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("Consulta Banco de Dados", "Erro na leitura do Banco de Dados: " + error.getMessage());

            }
        });
    }




    private void processarRegioes(List<Region> regionsFromDatabase) {
        avaliaDados (regionsFromDatabase);
    }



    public void avaliaDados(List<Region> listaBD) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(regions.isEmpty() && listaBD.isEmpty()){
            Log.d("Consulta Banco de Dados", " Lista e Banco Vazios ");
            semaphore.release();
            RegionUpdaterThread thread = new RegionUpdaterThread(regions,listaBD, newName, newlatitude, newlongitude, semaphore,true);
            thread.start();
        } else if(!regions.isEmpty() && listaBD.isEmpty()){
            Log.d("Consulta Banco de Dados", " Lista Cheia e Banco Vazio ");
            semaphore.release();
            RegionUpdaterThread thread = new RegionUpdaterThread(regions,listaBD, newName, newlatitude, newlongitude, semaphore,true);
            thread.start();
        }else if (regions.isEmpty() && !listaBD.isEmpty()){
            Log.d("Consulta Banco de Dados", " Lista Vazia e Banco Cheio ");
            semaphore.release();
            verificaBanco(listaBD);

        }else if (!regions.isEmpty() && !listaBD.isEmpty()) {
            Log.d("Consulta Banco de Dados", " Lista e Banco Cheios ");
            semaphore.release();
            boolean verifica = false;
            for (int i = 0; i < listaBD.size(); i++) {
                if (listaBD.get(i).getClass().equals(Region.class)) { //Verifica se alguma região(Region) do banco esta a mesnos de 30 metros de distancia do novo dado
                    boolean distancia = listaBD.get(i).calculateDistance(listaBD.get(i).getLatitude(), listaBD.get(i).getLongitude(), newlatitude, newlongitude);
                    if (distancia == false) {
                        verifica = true;
                        break; // Se encontrarmos uma região a menos de 30 metros, podemos sair do loop
                    }
                }
            }
            if (verifica == true){
                verificaBanco(listaBD);
            }else{ // passa somente os dados.
                RegionUpdaterThread thread = new RegionUpdaterThread(regions,listaBD, newName, newlatitude, newlongitude, semaphore,false);
                thread.start();
            }
        }
    }

    private void verificaBanco(List<Region> listaBD) {
        int indexRegiaoMenorQue30 = -1;
        for (int i = 0; i < listaBD.size(); i++) {
            if (listaBD.get(i) instanceof Region) {
                boolean distancia = listaBD.get(i).calculateDistance(listaBD.get(i).getLatitude(), listaBD.get(i).getLongitude(), newlatitude, newlongitude);
                if (distancia == false) {
                    indexRegiaoMenorQue30 = i;
                    break;
                }
            }
        }

        if (indexRegiaoMenorQue30 != -1) {
            if (indexRegiaoMenorQue30 == listaBD.size() - 1) {
                Log.d("Consulta Banco de Dados", " Adicionando SubRegion (Último elemento do banco)");
                RegionUpdaterThread thread = new RegionUpdaterThread(regions, listaBD,indexRegiaoMenorQue30, newName, newlatitude, newlongitude, semaphore, listaBD.get(indexRegiaoMenorQue30));
                thread.start();
            } else {
                boolean avalia = false;
                int posUltimoElementoAssociadoaRegion = -1;
                for (int i = indexRegiaoMenorQue30 +1; i < listaBD.size(); i++) {
                    if ((listaBD.get(i) instanceof SubRegion) || (listaBD.get(i) instanceof RestrictedRegion)) {
                        boolean distancia = listaBD.get(i).calculateDistance(listaBD.get(i).getLatitude(), listaBD.get(i).getLongitude(), newlatitude, newlongitude);
                        if (distancia == false) {
                            avalia = true;
                            break;
                        }
                    } else {
                        posUltimoElementoAssociadoaRegion = i - 1;
                        break;
                    }
                }
                if (avalia) {
                    Log.d("Consulta Banco de Dados", " Nova região não pode ser inserida (Distância menor que 5 metros detectada)");
                } else if ((posUltimoElementoAssociadoaRegion != -1) && (!avalia)) {
                    Log.d("Consulta Banco de Dados", " Encontrou uma Region após indexRegiaoMenorQue30 e nenhum elemento SubRegion ou RestrictedRegion associado a indexRegiaoMenorQue30 está a menos de 5 metros de distância da nova região 1");
                    verificaTipo(listaBD, posUltimoElementoAssociadoaRegion);
                } else if ((posUltimoElementoAssociadoaRegion == -1) && (!avalia)) {
                    Log.d("Consulta Banco de Dados", " Não encontrou uma Region após indexRegiaoMenorQue30 e nenhum elemento SubRegion ou RestrictedRegion associado a indexRegiaoMenorQue30 está a menos de 5 metros de distância da nova região 2");
                    verificaTipo(listaBD, listaBD.size() - 1);
                }
            }
        } else {
            Log.d("Consulta Banco de Dados", " Nenhuma região do banco está a menos de 30 metros de distância do novo dado");
            RegionUpdaterThread thread = new RegionUpdaterThread(regions,listaBD, newName, newlatitude, newlongitude, semaphore);
            thread.start();
        }
    }

    private void verificaTipo(List<Region> listaBD, int index) {
        if (listaBD.get(index) instanceof SubRegion){
            Log.d("Consulta Banco de Dados", " Adicionando RestrictedRegion");
            SubRegion subregion = (SubRegion)listaBD.get(index);
            Region mainRegion = subregion.getMainRegion();
            RegionUpdaterThread thread = new RegionUpdaterThread(regions,listaBD,index, newName, newlatitude, newlongitude, semaphore, true, mainRegion);
            thread.start();
        } else  if (listaBD.get(index) instanceof RestrictedRegion){
            Log.d("Consulta Banco de Dados", " Adicionando SubRegion");
            RestrictedRegion restrictedRegion = (RestrictedRegion) listaBD.get(index);
            Region mainRegion = restrictedRegion.getMainRegion();
            RegionUpdaterThread thread = new RegionUpdaterThread(regions,listaBD,index,newName, newlatitude, newlongitude, semaphore, mainRegion);
            thread.start();
        }else{
            Log.d("Consulta Banco de Dados", " Adicionando SubRegion");

            Region mainRegion = listaBD.get(index);
            RegionUpdaterThread thread = new RegionUpdaterThread(regions,listaBD,index, newName, newlatitude, newlongitude, semaphore, mainRegion);
            thread.start();
        }
    }

    public static String nomeSimplesUltimoElemento(List<?> listaBD, int index) {
        if (listaBD == null || listaBD.isEmpty()) {
            return null;
        } else {
            Object ultimoElemento = listaBD.get(index);
            return ultimoElemento.getClass().getSimpleName();
        }
    }




}