/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Connections;

import GUI.mainWindow;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Altair
 */
public class receiveFile extends Thread {

    private byte[] receivedData = new byte[8192];
    private BufferedInputStream streamNet;
    private BufferedOutputStream streamFile;
    
    private ServerSocket serverSocket;

    public receiveFile(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void run() {
        try {
            Socket socket = serverSocket.accept();

            receberFicheiro(socket);
        } catch (IOException ex) {
            //Logger.getLogger(receiveFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean waitForChoise(String filename) {
        mainWindow.receiveFile.setEnabled(false);

        mainWindow.mensagem.setText("Want to receive " + filename + "?");
        mainWindow.stop.setText("No");
        mainWindow.pause.setEnabled(true);
        mainWindow.pause.setText("Yes");

        while(true) {
            try {
                sleep(1000);
                if (mainWindow.accept != 0) {
                    mainWindow.stop.setText("Stop");
                    mainWindow.pause.setText("Pause");
                    if (mainWindow.accept == 1) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(receiveFile.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean existeFicheiro(File file) {
        File temp = new File(file.getName());

        if(temp.exists()) {
          //  System.out.println("O ficheiro existe!");
            return true;
        }

        return false;
    }


    private void receberFicheiro(Socket socket) {
        try {
            mainWindow.showAll();

            streamNet = new BufferedInputStream(socket.getInputStream());
            ObjectInputStream msg = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream outMsg = new ObjectOutputStream(socket.getOutputStream());

            File file = (File) msg.readObject();

            //Perguntar se quer aceitar o ficheiro?
            if(!waitForChoise(file.getName())) {
                outMsg.writeObject("nao");

                outMsg.close();
                streamNet.close();
                socket.close();
                msg.close();
                return;
            }
            outMsg.writeObject("sim");

            if(existeFicheiro(file)) {
                long tamanhoActual = new File(file.getName()).length();
                outMsg.writeObject("" + tamanhoActual);

                streamFile = new BufferedOutputStream(new FileOutputStream(file.getName(), true));
            }
            else {
                outMsg.writeObject("0");
                streamFile = new BufferedOutputStream(new FileOutputStream(file.getName()));
            }

            //Tamanho do ficheiro
            long tamanhoFicheiro = Long.parseLong((String) msg.readObject());

            mainWindow.stop.setText("Stop");

            int in;
            int perc = 0;


            File temp;
            int loop = 0;

            while ((in = streamNet.read(receivedData)) != -1 && !mainWindow.pauseSend) {
                //System.out.println("Recebi uma!");
                loop++;
                if(loop == 10) {
                    temp = new File(file.getName());
                    perc = (int) ((temp.length() * 100) / (tamanhoFicheiro));
                    mainWindow.mensagem.setText("Total Received: " + perc + "%");
                    GUI.mainWindow.progress.setValue(perc);

                    loop = 0;
                    temp = null;
                }

                streamFile.write(receivedData,0,in);
            }
            temp = new File(file.getName());

            if(temp.length() > tamanhoFicheiro) {
                mainWindow.mensagem.setText("File didn't finished! - " + perc + "%");
            }
            else if(mainWindow.pauseSend) {
                avisarPausa(outMsg, new File(file.getName()));
                mainWindow.mensagem.setText("Pause - " + perc + "%");
            }
            else {
                mainWindow.mensagem.setText("File Received!");
                mainWindow.progress.setValue(100);
            }
            
            mainWindow.stop.setText("Clear");
            mainWindow.pause.setEnabled(false);

            sleep(1000);

            outMsg.close();
            streamFile.close();
            msg.close();


        } catch (InterruptedException ex) {
            Logger.getLogger(receiveFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(receiveFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(receiveFile.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void avisarPausa(ObjectOutputStream out, File ficheiro) {
        try {
            out.writeObject("" + ficheiro.length());
        } catch (IOException ex) {
            Logger.getLogger(receiveFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
