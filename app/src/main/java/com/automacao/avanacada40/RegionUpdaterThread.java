package com.automacao.avanacada40;


import android.util.Log;

import com.example.biblioteca.Region;
import com.example.biblioteca.RestrictedRegion;
import com.example.biblioteca.SubRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class RegionUpdaterThread extends Thread {
    private List<Region> regions;
    private String newName;
    private double newlatitude;
    private double newlongitude;

    private Semaphore semaphore;
    private Random random = new Random();
    private boolean restricted = false;
    private Region mainRegion = null;
    private RestrictedRegion restrictedRegion = null;
    private Region region = null;
    private  SubRegion subRegion = null;
    private boolean bancovasio = false;
    private List<Region> listaBD = new ArrayList<>();
    private int index;



    public RegionUpdaterThread( List<Region> regions, List<Region> listaBD, int index,String locationName, double latitude, double longitude, Semaphore semaphore, boolean restricted, Region mainRegion) {
        this.regions = regions;
        this.newName = locationName;
        this.newlatitude = latitude;
        this.newlongitude = longitude;
        this.semaphore = semaphore;
        this.restricted = restricted;
        this.mainRegion = mainRegion;
        this.listaBD = listaBD;
        this.index = index;
        this.restrictedRegion = new RestrictedRegion(locationName, latitude, longitude, Math.abs(random.nextInt()), System.nanoTime(), restricted, mainRegion);

    }
    public RegionUpdaterThread( List<Region> regions,List<Region> listaBD, int index, String locationName, double latitude, double longitude, Semaphore semaphore, Region mainRegion) {
        this.regions = regions;
        this.newName = locationName;
        this.newlatitude = latitude;
        this.newlongitude = longitude;
        this.semaphore = semaphore;
        this.mainRegion = mainRegion;
        this.listaBD = listaBD;
        this.index = index;
        this.subRegion = new SubRegion(locationName, latitude, longitude, Math.abs(random.nextInt()), System.nanoTime(), mainRegion);

    }
    public RegionUpdaterThread( List<Region> regions,List<Region> listaBD, String locationName, double latitude, double longitude, Semaphore semaphore) {
        this.regions = regions;
        this.newName = locationName;
        this.newlatitude = latitude;
        this.newlongitude = longitude;
        this.semaphore = semaphore;
        this.listaBD = listaBD;
        this.index = index;
        this.region = new Region(locationName, latitude, longitude, System.nanoTime(), Math.abs(random.nextInt()));
    }
    public RegionUpdaterThread( List<Region> regions,List<Region> listaBD, String locationName, double latitude, double longitude, Semaphore semaphore,boolean bancovasio) {
        this.regions = regions;
        this.newName = locationName;
        this.newlatitude = latitude;
        this.newlongitude = longitude;
        this.semaphore = semaphore;
        this.bancovasio = bancovasio;
        this.listaBD = listaBD;
        this.region = new Region(locationName, latitude, longitude, System.nanoTime(), Math.abs(random.nextInt()));

    }


    @Override
    public void run() {
        long startTime = System.nanoTime();
        // Adquira a permissão do semáforo antes de acessar a lista
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        avaliaDados();

        // Registra o tempo de término do salvamento
        long endTime = System.nanoTime();
        // Calcula o tempo decorrido em nanossegundos
        long elapsedTime = endTime - startTime;
        // Use o tempo decorrido conforme necessário, como registrá-lo em logs ou realizar outras ações
        Log.d("Consulta Lista", "Tempo decorrido para Salvar na lista " + elapsedTime + " nanossegundos");

    }



    public void avaliaDados() {

        if (regions.isEmpty() && bancovasio == true && region != null) {
            Log.d("Consulta Lista", " Lista e Banco Vazia ");
            regions.add(region);
            Log.d("Consulta Lista", "Região adicionada: " + region.getName());
            semaphore.release();

        } else if (regions.isEmpty() && bancovasio == false) {
            Log.d("Consulta Lista", " Lista Vazia e Banco Cheio ");
            if (restrictedRegion != null) {
                listaBD.add(index+1,restrictedRegion);
                listaBD.addAll(regions);
                regions.clear();
                regions.addAll(listaBD);
                System.out.println("Elementos da lista:");
                imprimirElementos(regions);
                Log.d("Consulta Lista", "Região restrita adicionada: " + restrictedRegion.getName());
                restrictedRegion = null;
            }
            if (region != null) {
                listaBD.add(region);
                listaBD.addAll(regions);
                regions.clear();
                regions.addAll(listaBD);
                System.out.println("Elementos da lista:");
                imprimirElementos(regions);
                Log.d("Consulta Lista", "Região adicionada: " + region.getName());
                region=null;
            }
            if (subRegion != null) {
                listaBD.add(index+1,subRegion);
                listaBD.addAll(regions);
                regions.clear();
                regions.addAll(listaBD);
                System.out.println("Elementos da lista:");
                imprimirElementos(regions);
                Log.d("Consulta Lista", "Sub-região adicionada: " + subRegion.getName());
                subRegion = null;
            }
            semaphore.release();

        } else if (!regions.isEmpty() && bancovasio == true) {
            Log.d("Consulta Lista", " Lista Cheia e Banco Vazio ");
            verificaLista(regions);
            semaphore.release();

        } else if (!regions.isEmpty() && bancovasio == false) {
            Log.d("Consulta Lista", " Lista Cheia e Banco Cheio ");

            if (restrictedRegion != null && region != null && subRegion != null && bancovasio == false) {
                verificaLista(regions);
            }
            if (restrictedRegion != null) {
                try {
                    regions.add(buscarIndiceElemento(regions,listaBD.get(index))+1,restrictedRegion);
                }catch (Exception e){
                    Log.d("Consulta Lista", "Elemento invalido");
                }
                System.out.println("Elementos da lista:");
                imprimirElementos(regions);
                Log.d("Consulta Lista", "Região restrita adicionada: " + restrictedRegion.getName());
                restrictedRegion = null;
            }
            if (region != null) {
                verificaLista(regions);
            }
            if (subRegion != null) {
                try {
                    regions.add(buscarIndiceElemento(regions,listaBD.get(index))+1,subRegion);

                }catch (Exception e){
                    Log.d("Consulta Lista", "Elemento invalido");
                }
                System.out.println("Elementos da lista:");
                imprimirElementos(regions);
                Log.d("Consulta Lista", "Sub-região adicionada: " + subRegion.getName());
                subRegion = null;
            }
            semaphore.release();
        }
    }

    private void verificaLista(List<Region> lista) {
        int indexRegiaoMenorQue30 = -1;
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i) instanceof Region) {
                boolean distancia = lista.get(i).calculateDistance(lista.get(i).getLatitude(), lista.get(i).getLongitude(), newlatitude, newlongitude);
                Log.d("Consulta Lista", "Distância da região " + i + " : " + distancia + " metros.");
                if (distancia == false) {
                    indexRegiaoMenorQue30 = i;
                    break;
                }
            }
        }

        if (indexRegiaoMenorQue30 != -1) {
            if (indexRegiaoMenorQue30 == lista.size() - 1) {
                Log.d("Consulta Lista", "Adicionando SubRegion (Último elemento da lista)");
                SubRegion newSubRegion = new SubRegion(newName, newlatitude, newlongitude, Math.abs(random.nextInt()), System.nanoTime(), lista.get(indexRegiaoMenorQue30));
                regions.add(indexRegiaoMenorQue30 +1,newSubRegion);
                System.out.println("Elementos da lista:");
                imprimirElementos(regions);
            } else {

                boolean avalia = false;
                int posUltimoElementoAssociadoaRegion = -1;
                for (int i = indexRegiaoMenorQue30 +1; i < lista.size(); i++) {
                    if ((lista.get(i) instanceof SubRegion) || (lista.get(i) instanceof RestrictedRegion)) {
                       boolean distancia = lista.get(i).calculateDistance(lista.get(i).getLatitude(), lista.get(i).getLongitude(), newlatitude, newlongitude);
                        Log.d("Consulta Lista", "Distância do elemento após região " + indexRegiaoMenorQue30 + " : " + distancia + " metros.");
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
                    Log.d("Consulta Lista", "Nova região não pode ser inserida (Distância menor que 5 metros detectada)");
                } else if ((posUltimoElementoAssociadoaRegion != -1) && (!avalia)) {
                    Log.d("Consulta Lista", "Encontrou uma Region após indexRegiaoMenorQue30 e nenhum elemento SubRegion ou RestrictedRegion associado a indexRegiaoMenorQue30 está a menos de 5 metros de distância da nova região 1");
                    verificaTipo(lista, posUltimoElementoAssociadoaRegion);
                } else if ((posUltimoElementoAssociadoaRegion == -1) && (!avalia)) {
                    Log.d("Consulta Lista", "Não encontrou uma Region após indexRegiaoMenorQue30 e nenhum elemento SubRegion ou RestrictedRegion associado a indexRegiaoMenorQue30 está a menos de 5 metros de distância da nova região 2");
                    verificaTipo(lista, lista.size() - 1);
                }
            }
        } else {
            Log.d("Consulta Lista", "Nenhuma região da lista está a menos de 30 metros de distância do novo dado");
            Region newRegion = new Region(newName, newlatitude, newlongitude, System.nanoTime(), Math.abs(random.nextInt()));
            regions.add(newRegion);
            listaBD.addAll(regions);
            regions.clear();
            regions.addAll(listaBD);
            System.out.println("Elementos da lista:");
            imprimirElementos(regions);
        }
    }

    private void verificaTipo(List<Region> lista, int index) {
        if (lista.get(index) instanceof SubRegion) {
            Log.d("Consulta Lista", "Adicionando RestrictedRegion");
            SubRegion subregion = (SubRegion)lista.get(index);
            Region mainRegion = subregion.getMainRegion();
            RestrictedRegion restrictedRegion = new RestrictedRegion(newName, newlatitude, newlongitude, Math.abs(random.nextInt()), System.nanoTime(), true, mainRegion);
            regions.add(index +1,restrictedRegion);
            System.out.println("Elementos da lista:");
            imprimirElementos(regions);
        } else if (lista.get(index) instanceof RestrictedRegion){
            Log.d("Consulta Lista", "Adicionando SubRegion");
            RestrictedRegion restrictedRegion = (RestrictedRegion) lista.get(index);
            Region mainRegion = restrictedRegion.getMainRegion();
            SubRegion subRegion = new SubRegion(newName, newlatitude, newlongitude, Math.abs(random.nextInt()), System.nanoTime(), mainRegion);
            regions.add(index +1,subRegion);
            System.out.println("Elementos da lista:");
            imprimirElementos(regions);
        }else{
            Log.d("Consulta Lista", "Adicionando SubRegion");
            Region mainRegion = lista.get(index);
            SubRegion subRegion = new SubRegion(newName, newlatitude, newlongitude, Math.abs(random.nextInt()), System.nanoTime(), mainRegion);
            regions.add(index +1,subRegion);
            System.out.println("Elementos da lista:");
            imprimirElementos(regions);
        }
    }

    public static String nomeSimplesUltimoElemento(List<?> lista, int index) {
        if (lista == null || lista.isEmpty()) {
            return null;
        } else {
            Object ultimoElemento = lista.get(index);
            return ultimoElemento.getClass().getSimpleName();
        }
    }

    public static void imprimirElementos(List<Region> lista) {

        for (Region elemento : lista) {
            Log.d("Consulta Lista", "Tipo: " + nomeSimplesUltimoElemento(lista, lista.lastIndexOf(elemento)));
        }
    }
    public static <T> int buscarIndiceElemento(List<T> lista, T elemento) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).equals(elemento)) {
                return i; // Retorna o índice se o elemento for encontrado
            }
        }
        return -1; // Retorna -1 se o elemento não for encontrado na lista
    }

}




