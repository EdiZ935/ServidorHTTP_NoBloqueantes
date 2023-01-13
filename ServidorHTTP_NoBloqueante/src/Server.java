import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.*;

public class Server{
    public static final int port = 8000;
    protected static int BuffSize=10240;
    public static void main(String[] args){
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            ssc.bind(new InetSocketAddress(port));
            Selector selector = Selector.open();
            ssc.register(selector,SelectionKey.OP_ACCEPT);
            System.out.println("Servidor iniciado...\n\n");

            while (true){
                selector.select();
                Iterator<SelectionKey>iterador= selector.selectedKeys().iterator();
                while (iterador.hasNext()){
                    SelectionKey key = iterador.next();
                    iterador.remove();
                    if(key.isAcceptable()){
                        SocketChannel sc = ssc.accept();
                        sc.configureBlocking(false);
                        sc.register(selector,SelectionKey.OP_READ);
                        System.out.println("\nCliente Conectado desde: "+sc.socket().getInetAddress().getHostAddress());
                        System.out.println("Por el puerto: "+sc.socket().getPort());
                    } else if (key.isReadable()) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        ByteBuffer bb = ByteBuffer.allocate(BuffSize);
                        int b_leidos;
                        String linea;
                        bb.clear();
                        b_leidos = sc.read(bb);
                        bb.flip();
                        if(b_leidos == -1)
                            linea = "";
                        else
                            linea = new String(bb.array(),0,b_leidos);
                        System.out.println("linea: " + linea);
                        Metodos metodos = new Metodos(sc);
                        metodos.manejador(linea);
                        System.out.println("Cliente atendido: "+sc.socket().getInetAddress().getHostAddress()+"\n\n");
                        sc.close();
                    }
                }//End While
            }//End while...
        }catch (Exception e){
            e.printStackTrace();
        }
    }//Fin Main...
        public static class Metodos{
            protected SocketChannel channel;
            protected ByteBuffer Bbuff;
            protected int BuffSize=10240;
            int codeNumber = 200;
            String response = "OK";
            String path = "src/ContenidoServer/";
            protected String FileName = "";
            protected Dictionary<String, String> MIME = new Hashtable<>();
            protected String deleteHtml_Ok ="<html><head><meta charset='UTF-8'><title>"+codeNumber+"  </title></head>"
                                                + "<body><h1>  </h1>"
                                                + "<p>Elemento: "+ FileName +" fue eliminado exitosamente mediante DELETE</p>"
                                                + "</body></html>";
            protected String E404 = "HTTP/1.1 404 Not Found\n"
                                        + "Date: "+new Date()+"\n"
                                        + "Server: EGZ_KYF Server/1.0\n"
                                        + "Content-Type: text/html \n\n"
                    + "<html><head><meta charset='UTF-8'><title>404 NOT FOUND  </title></head>"
                    + "<body><h1> Puede tratarse de una página eliminada que no tiene reemplazo o equivalente"
                    + " o se trata de una página que simplemente no existe </h1>"
                    ;
            protected final String E500 = "<html><head><meta charset='UTF-8'><title>500 INTERNAL SERVER ERROR  </title></head>"
                                        + "<body><h1> UPS, OCURRIÓ UN ERROR INESPERADO</h1>"
                                        + "<p>No se pudo concretar la operación</p>"
                                        + "</body></html>";
            protected final String E403 = "<html><head><meta charset='UTF-8'><title>403 FORBIDDEN  </title></head>"
                                        + "<body><h1>The server understands the request but refuses to authorize it</h1>"
                                        + "<p>The access is tied to the application logic, such as insufficient rights to a resource.</p>"
                                        + "</body></html>";
            public Metodos(SocketChannel SC){
                this.channel = SC;
                this.MIME.put("txt", "text/plain");
                this.MIME.put("html", "text/html");
                this.MIME.put("htm", "text/html");
                this.MIME.put("jpg", "image/jpeg");
                this.MIME.put("jpeg", "image/jpeg");
                this.MIME.put("png", "image/png");
                this.MIME.put("pdf", "application/pdf");
                this.MIME.put("doc", "application/msword");
                this.MIME.put("rar", "application/x-rar-compressed");
                this.MIME.put("mp3", "audio/mpeg");
                this.MIME.put("mp4", "video/mp4");
                this.MIME.put("c", "text/plain");
                this.MIME.put("java", "text/plain");
            }
            public void manejador(String linea){
                try {
                    if (linea.toUpperCase().startsWith("GET")) // Caso que la petición empiece con la palabra "GET"
                    {
                        GET(linea);//Se ejecuta el método GET usando la variable global actualizada 'FileName'
                    }
                    else if(linea.toUpperCase().startsWith("DELETE"))// Caso que la petición empiece con la palabra "DELETE"
                    {
                        DELETE(linea);//Se ejecuta el método DELETE usando la variable global actualizada 'FileName'
                    }
                    else if (linea.toUpperCase().startsWith("HEAD"))
                    {
                        HEAD(linea);//Se ejecuta el método HEAD usando la variable global actualizada 'FileName'
                    }
                    else if (linea.toUpperCase().startsWith("POST"))
                    {
                        System.out.println("Recibiendo POST...");
                        String request;
                        if(linea.contains("=")){
                            request = linea;
                            String htmlPost = POST(request);// Se manda a llamar la función POST recuperando una String que representa la respuesta
                            System.out.println("Respuesta:");
                            System.out.println(htmlPost);
                            Bbuff = ByteBuffer.wrap(htmlPost.getBytes());
                            channel.write(Bbuff);
                        }else{
                            String temp = "HTTP/1.0 501 Not Implemented";
                            Bbuff = ByteBuffer.wrap(temp.getBytes());
                            channel.write(Bbuff);
                        }
                    }
                    else if (!linea.contains("?"))
                    {
                        getFileName(linea);
                        if(FileName.compareTo("")==0)
                            SendF(path+"index.html");
                        else
                            SendF(path+FileName);
                    }
                    else
                    {
                        String temp = "HTTP/1.0 501 Not Implemented";
                        Bbuff = ByteBuffer.wrap(temp.getBytes());
                        channel.write(Bbuff);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }//Fin Método Manejador
            public void DELETE(String linea){
                getFileName(linea);
                try{
                    System.out.println("Petición de eliminado del archivo: " + FileName);
                    File file = new File(path+FileName);
                    if(file.exists()){
                        if(file.delete()){//Si fue posible eliminarlo:
                            System.out.println("Petición de eliminado de: "+ FileName +"   ha sido exitosa.");
                            this.codeNumber = 200;
                            this.response = "OK";
                            String contentType = "text/html";
                            String headerHTTP = "HTTP/1.1 "+ 200 +" "+"OK"+"\n"
                                    + "Date: "+new Date()+"\n"
                                    + "Server: EGZ_KYF Server/1.0\n"
                                    + "Content-Type: "+contentType+" \n\n";

                            String temp = headerHTTP+deleteHtml_Ok;
                            Bbuff = ByteBuffer.wrap(temp.getBytes());
                            channel.write(Bbuff);
                        }else{

                            String contentType = "text/html";
                            String headerHTTP = "HTTP/1.1 "+ 500 +" "+"Internal Server Error"+"\n"
                                    + "Date: "+new Date()+"\n"
                                    + "Server: EGZ_KYF Server/1.0\n"
                                    + "Content-Type: "+contentType+" \n";

                            String temp = headerHTTP+"\n"+E500;
                            Bbuff = ByteBuffer.wrap(temp.getBytes());
                            channel.write(Bbuff);
                        }
                    }else{//Si no existe el archivo....
                        System.out.println("Petición de eliminado de: "+FileName+"   ha fracasado. NO SE ENCONTRÓ");
                        this.codeNumber = 404;
                        this.response = "Not Found";

                        String temp = E404+ "<p>Elemento: "+ FileName +" NO EXISTE O FUE ELIMINADO</p>"
                                + "</body></html>";
                        Bbuff = ByteBuffer.wrap(temp.getBytes());
                        channel.write(Bbuff);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }//Fin del método DELETE
            public void GET(String linea){
                getFileName(linea);
                try {
                    File temp = new File(path+FileName);
                    if (temp.exists()) {
                        if (SendF(path+FileName)) {//Si la función de mandado de archivo regresa true:
                            System.out.println("Petición de lectura de: " + FileName + "   ha sido exitosa.");
                        } else {//Si hubo un error en el enviado del archivo solicitado:
                            System.out.println("Petición de lectura de: " + FileName + "   ha fracasado.");
                            String contentType = "text/html";
                            String headerHTTP = "HTTP/1.1 "+ 500 +" "+"Internal Server Error"+"\n"
                                    + "Date: "+new Date()+"\n"
                                    + "Server: EGZ_KYF Server/1.0\n"
                                    + "Content-Type: "+contentType+" \n";

                            String tempS = headerHTTP+"\n"+E500;
                            Bbuff = ByteBuffer.wrap(tempS.getBytes());
                            channel.write(Bbuff);
                        }
                    } else {
                        System.out.println("Petición de lectura de: " + FileName + "   ha fracasado. NOT FOUND");
                        this.codeNumber = 404;
                        Bbuff = ByteBuffer.wrap(E404.getBytes());
                        channel.write(Bbuff);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }//Fin método GET
            public String POST(String request) {
                int indice = request.indexOf("/");
                if(request.contains("/")){
                    if(request.contains("?"))
                        request = request.substring(indice+2);
                    else
                        request = request.substring(indice+1);
                    StringTokenizer post = new StringTokenizer(request," ");
                    request = post.nextToken();
                }
                String[] reqLineas = request.split("\n");
                StringTokenizer tokens = new StringTokenizer(reqLineas[reqLineas.length-1], "&?");
                System.out.println(reqLineas[reqLineas.length-1]);

                String contentType = "text/html";
                String headerHTTP = "HTTP/1.1 "+ "200" +" "+"OK"+"\n"
                        + "Date: "+new Date()+"\n"
                        + "Server: EGZ_KYF Server/1.0\n"
                        + "Content-Type: "+contentType+" \n\n";
                StringBuilder html = new StringBuilder(headerHTTP
                        + "<html><head><meta charset='UTF-8'><title> Método POST </title></head>\n"
                        + "<body ><center><h2> Se han obtenido los siguientes valores con sus respectivos parámetros</h2><br>\n"
                        + "<table border='2'><tr><th>Valores:</th><th>Valor</th></tr>");

                while (tokens.hasMoreTokens()) {
                    String postValues = tokens.nextToken();
                    System.out.println(postValues);
                    StringTokenizer postValue = new StringTokenizer(postValues, "=");
                    String parametro = "";
                    String valor = "";
                    if (postValue.hasMoreTokens()) {
                        parametro = postValue.nextToken();
                    }
                    if (postValue.hasMoreTokens()) {
                        valor = postValue.nextToken();
                    }
                    html.append("<tr><td><b>").append(parametro).append("</b></td><td>").append(valor).append("</td></tr>\n");
                }
                html.append("</table></center></body></html>");
                return html.toString();
            }//Fin POST: Regresa html String: Mandar en respuesta
            public void HEAD(String linea)throws Exception{
                getFileName(linea);
                File file = new File(path+FileName);

                if (!linea.contains("?")||file.exists()) {
                    //Se actualiza el FileName según la línea de petición recibida
                    if (file.isDirectory()){
                        this.FileName = "page403.html";
                        int codeNumber = 403;
                        String response = "Forbidden\n";
                        String contentType = "text/html";
                        String headerHTTP = "HTTP/1.1 "+ codeNumber +" "+response+"\n"
                                + "Date: "+new Date()+"\n"
                                + "Server: EGZ_KYF Server/1.0\n"
                                + "Content-Type: "+contentType+" \n";

                        String temp = headerHTTP+"\n"+E403;
                        Bbuff = ByteBuffer.wrap(temp.getBytes());
                        channel.write(Bbuff);
                    }else{
                        int posExt = FileName.indexOf(".")+1;
                        String ext = FileName.substring(posExt);
                        String contentType = MIME.get(ext);
                        String headerHTTP = "HTTP/1.1 "+ codeNumber +" "+response+"\n"
                                + "Date: "+new Date()+"\n"
                                + "Server: EGZ_KYF Server/1.0\n"
                                + "Content-Type: "+contentType+" \n";

                        String temp = headerHTTP+"Content-Length: " + file.length() +" \n\n";
                        Bbuff = ByteBuffer.wrap(temp.getBytes());
                        channel.write(Bbuff);
                        System.out.println(headerHTTP+"Content-Length: " + file.length() +" \n\n");
                    }

                }else if(!file.exists()){
                    this.FileName = "page404.html";
                    this.codeNumber = 404;
                    this.response = "Not Found";

                    String temp = E404+ "<p>Elemento: "+ FileName +" NO EXISTE O FUE ELIMINADO</p>"
                            + "</body></html>";
                    Bbuff = ByteBuffer.wrap(temp.getBytes());
                    channel.write(Bbuff);


                }else {
                    this.codeNumber = 200;
                    this.response = "OK";
                    String contentType = "text/html";
                    String headerHTTP = "HTTP/1.1 "+ codeNumber +" "+response+"\n"
                            + "Date: "+new Date()+"\n"
                            + "Server: EGZ_KYF Server/1.0\n"
                            + "Content-Type: "+contentType+" \n";

                    Bbuff = ByteBuffer.wrap(headerHTTP.getBytes());
                    channel.write(Bbuff);
                }

            }//Fin HEAD
            void getFileName(String comando){//Actualiza la variable global FileName
                int i = comando.indexOf("/");
                if(comando.indexOf("?")==(i+1)){
                    i++;
                }
                int f = comando.indexOf(" ", i);
                this.FileName = comando.substring(i + 1, f);
            }//Fin getFileName: Escribe la variable global con los datos recuperados
            public boolean SendF(String ruta){//Función Send File
                File f = new File(ruta);
                System.out.println(ruta);//ruta
                System.out.println(f.exists());
                System.out.println(f.isDirectory());
                if(f.exists() && f.isFile()){
                    try{
                        int posExt = FileName.indexOf(".")+1;
                        String ext = FileName.substring(posExt);
                        ByteBuffer Buff = ByteBuffer.allocate( BuffSize );
                        FileInputStream fis = new FileInputStream( ruta );
                        FileChannel fc = fis.getChannel();

                        int tam_archivo = fis.available();
                        String head = "HTTP/1.0 202 Accepted\n"
                                + "Server: EGZ_KYF Server/1.0 \n"
                                + "Date: " + new Date()+" \n"
                                + "Content-Type: "+MIME.get(ext)+" \n"
                                + "Content-Length: "+tam_archivo+" \n\n";
                        /*
                        Buff.clear();
                        Buff.put(0,head.getBytes());
                        channel.write(Buff);
                         */
                        Bbuff = ByteBuffer.wrap(head.getBytes());
                        channel.write(Bbuff);
                        Buff.clear();

                        int leidos=0;
                        int lectura;
                        while((lectura=fc.read(Buff))!=-1){
                            Buff.flip();
                            while (Buff.hasRemaining()){
                                channel.write(Buff);
                            }
                            Buff.clear();
                            leidos=leidos+lectura;
                        }//end while
                        System.out.println("Se han enviado: "+leidos+"\n");
                        fc.close();
                        fis.close();

                    }catch (Exception e){
                        e.printStackTrace();
                        return false;
                    }

                } else if (f.isDirectory()){//error 403
                    System.out.println("Error 403 Forbidden");
                }else{//error 404 NOT FOUND
                    System.out.println("Error 404 NOT Found");
                }//fin de los casos de uso
                return true;
            }//Fin SendF: Escribe en el buffer el archivo

        }//Término de MÉTODOS Class...
}
