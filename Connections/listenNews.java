/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Connections;

import GUI.mainWindow;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 * @author altair
 */
public class listenNews extends Thread {
    private ObjectInputStream in;
    private long tamanho;

    /**
     * 
     * @param in
     * @param tamanho
     */
    public listenNews(ObjectInputStream in, long tamanho) {
        this.in = in;
        this.tamanho = tamanho;
    }

    public void run() {
        String response;
        while(!mainWindow.end) {
            try {
              //  System.out.println("Ate agora tudo bem!");
                response = (String) in.readObject();
               // System.out.println("Ate agora tudo bem!");
                long tamanhoEnviado = Long.parseLong(response);
               // System.out.println("Ate agora tudo bem!");

                //Nao acabou
                if(tamanhoEnviado < tamanho) {
                    mainWindow.end = false;
                    mainWindow.pauseSend = true;
                    //System.out.println("Recebi o pedido de pause");
                }
                else {
                    mainWindow.end = true;
                    //System.out.println("Pelos vistos acabou");
                }
                
                break;

            } catch (IOException ex) {
                //System.out.println("Deu merda aqui!\n" + ex.getMessage());
                return;
            } catch (ClassNotFoundException ex) {
                return;
            }
        }
    }

}
