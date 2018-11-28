/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Connections;

import GUI.mainWindow;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Altair
 */
public class sendFile extends Thread {

    private BufferedInputStream streamFicheiro;
    private BufferedOutputStream streamNet;

    private byte[] byteArray = new byte[8192];
    private File file;
    private Socket socket;

    private boolean dontSend = false;

    public sendFile(String host, File file) {
        try {
            socket = new Socket(host, 7669);
            this.file = file;
        }catch (java.net.ConnectException ex) {
            mainWindow.mensagem.setText("Cannot send file to that host!");
            dontSend = true;
        }catch (UnknownHostException ex) {
            //Logger.getLogger(sendFile.class.getName()).log(Level.SEVERE, null, ex);
            mainWindow.mensagem.setText("Cannot send file to that host!");
            dontSend = true;
        } catch (IOException ex) {
            Logger.getLogger(sendFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Espera pela confirmacao em que o cliente aceita o ficheiro
     * @param in
     * @return false nao aceita, true aceita
     */
    private boolean esperarConfirmacao(ObjectInputStream in) {
        try {
            if (((String) in.readObject()).equals("nao")) {
                mainWindow.mensagem.setText("Cannot send file to that host!");
                mainWindow.stop.setText("Clear");
                mainWindow.pause.setEnabled(false);
                streamFicheiro.close();
                streamNet.close();
                in.close();
                
                return false;
            }
            return true;
        } catch (IOException ex) {
            Logger.getLogger(sendFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(sendFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Espera pela resposta do cliente se ja tem parte ou nao do ficheiro
     * @param in
     * @return
     */
    private long fazerResume(ObjectInputStream in) {
        try {
            return Long.parseLong((String) in.readObject());
        } catch (IOException ex) {
            Logger.getLogger(sendFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(sendFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    public void run() {
        if (!dontSend) {
            ObjectOutputStream msg = null;
            ObjectInputStream inMsg = null;

            try {
                GUI.mainWindow.showAll();

                mainWindow.disableFields();

                //Enviar o nome do ficheiro e etcs
                msg = new ObjectOutputStream(socket.getOutputStream());
                msg.writeObject(file);

                mainWindow.mensagem.setText("Waiting for response...");

                inMsg = new ObjectInputStream(socket.getInputStream());

                //Esperar pela confirmacao
                if(!esperarConfirmacao(inMsg)) {
                    msg.close();
                    return;
                }

                //O tamanho do ficheiro a ser enviado
                long tamanhoFicheiro = file.length();

                // Ver se e resume ou limpo
                long tamanhoActual = fazerResume(inMsg);

               // System.out.println("O gajo ja tem alguma coisa? " + tamanhoActual);

                //Enviar o tamanho do ficheiro
                msg.writeObject("" + tamanhoFicheiro);

                //Fica a ver se o gajo diz alguma coisa
                new listenNews(inMsg, tamanhoFicheiro).start();

                streamFicheiro = new BufferedInputStream(new FileInputStream(file.getAbsolutePath()));
                streamNet = new BufferedOutputStream(socket.getOutputStream());

                int in = 0;
                long total = 0;
                int perc;

                int ciclo = 0;

                while ((in = streamFicheiro.read(byteArray)) != -1 && !mainWindow.pauseSend) {
                    total += 8192;

                   // System.out.println("O enviado: " + total + " ,meta: " + tamanhoActual);

                    if(total > tamanhoActual) {
                       // System.out.println("Enviei uma!");
                        streamNet.write(byteArray, 0, in);
                    }

                    ciclo++;
                    if(ciclo == 10) {
                        perc = (int) ((total * 100) / (tamanhoFicheiro));
                        mainWindow.mensagem.setText("Total sent: " + perc + "%");
                        GUI.mainWindow.progress.setValue(perc);
                        ciclo = 0;
                    }

                }

                streamFicheiro.close();
                streamNet.close();
                inMsg.close();
                msg.close();

                if(mainWindow.pauseSend) {
                    mainWindow.mensagem.setText("Download paused, send again later to complete.");
                }
                else {
                    mainWindow.mensagem.setText("File Sent!");
                    mainWindow.progress.setValue(100);
                }

                mainWindow.stop.setText("Clear");
                mainWindow.pause.setEnabled(false);

                

            } catch (IOException ex) {
                try {
                    if(mainWindow.pauseSend) {
                        mainWindow.mensagem.setText("Download paused, send again later to complete.");
                    }
                    else {
                        mainWindow.mensagem.setText("Cannot send file to that host!");
                    }
                    
                    mainWindow.stop.setText("Clear");
                    mainWindow.pause.setEnabled(false);

                    streamFicheiro.close();
                    streamNet.close();
                    inMsg.close();
                    msg.close();
                    
                    return;
                } catch (IOException ex1) {
                    Logger.getLogger(sendFile.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }
}
