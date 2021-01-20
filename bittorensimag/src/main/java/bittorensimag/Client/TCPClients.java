package bittorensimag.Client;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

//le but de ce code est de faire l'équivalent du serveur TCPServerSelector
// mais coté client : gérer 3 connexions (sockets actives) basées sur des sockets non bloquantes 
// Chacune ainsi
// - envoie la chaine de caractère "hello" au serveur 
// - récupéré du serveur "hello"
// - et l'affiche
// pour le lancer java TCPClients 127.0.0.1 "n'importe quoi" 1000. 
// (Le 2ème argument n'a pas d'importance)

public class TCPClients {

  public static void main(String args[]) throws Exception {

    if ((args.length < 2) || (args.length > 3)) // Test for correct # of args
      throw new IllegalArgumentException("Parameter(s): <Server> <Word> [<Port>]");

    String server = args[0]; // Server name or IP address
    // Convert input String to bytes using the default charset
    byte[] argument = args[1].getBytes();

    int servPort = (args.length == 3) ? Integer.parseInt(args[2]) : 7;
    Selector selector = Selector.open();

    // Je vais lancer 3 connexions TCP clientes vers le serveur TCPServerSelector

    for (int i = 0; i < 3; i++) {
      SocketChannel clntChan = SocketChannel.open();
      clntChan.configureBlocking(false);
      clntChan.register(selector, SelectionKey.OP_CONNECT);
      // launch a connection to a peer
      clntChan.connect(new InetSocketAddress(server, servPort));
    }

    while (true) { // Run forever, processing available I/O operations
      // Wait for some channel to be ready (or timeout)
      if (selector.select(3000) == 0) { // returns # of ready chans
        System.out.print(".");
        continue;
      }

      // Get iterator on set of keys with I/O to process
      Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
      while (keyIter.hasNext()) {
        SelectionKey key = keyIter.next(); // Key is bit mask

        if (key.isConnectable()) {
          // key.channel() me permet de récupérer la socket
          // sur laquelle l'évènement OP_CONNECT est arrivée
          // et ainsi faire un traitement personnalisé
          SocketChannel clntChan = (SocketChannel) key.channel();
          // Finishes the process of connecting a socket channel
          clntChan.finishConnect();
          // une fois la socket bien connectée, j'indique qu'au prochain select()
          // je suis intéressé par l'évènement OP_WRITE
          // (possibilité d'écrire sur ma socket ce qui est quasiment toujours vrai)
          key.interestOps(SelectionKey.OP_WRITE);

        }

        // Client socket channel is available for writing and
        // key is valid (i.e., channel not closed)?
        if (key.isValid() && key.isWritable()) {

          SocketChannel clntChan = (SocketChannel) key.channel();
          // J'envoie la chaine de caractère "Hello" au serveur TCP
          ByteBuffer writeBuf = ByteBuffer.wrap("hello".getBytes());
          while (writeBuf.hasRemaining()) {
            clntChan.write(writeBuf);
          }
          // j'aimerai qu'on m'avertisse au prochain select si des données sont arrivées
          // sur cette socket
          key.interestOps(SelectionKey.OP_READ);

        }

        // Client socket channel has pending data?
        // y a-t-il des données qui sont arrivées sur le buffer de réception de ma
        // socket ?
        if (key.isReadable()) {
          // je récupère la socket
          SocketChannel clntChan = (SocketChannel) key.channel();
          ByteBuffer readBuf = ByteBuffer.allocate("hello".length());
          int totalBytesRcvd = 0; // Total bytes received so far
          int bytesRcvd; // Bytes received in last read
          while (totalBytesRcvd < "hello".length()) {
            // je lis les données dans le buffer de réception de ma socket
            // normalement le serveur me renvoie "hello"
            if ((bytesRcvd = clntChan.read(readBuf)) == -1) {
              throw new SocketException("Connection closed prematurely");
            }
            totalBytesRcvd += bytesRcvd;
            System.out.print("."); // Do something else
          }

          System.out.println("Received: " + // convert to String per default charset
              new String(readBuf.array(), 0, totalBytesRcvd));
          clntChan.close();
        }

        keyIter.remove(); // remove from set of selected keys
      }
    }
  }
}
