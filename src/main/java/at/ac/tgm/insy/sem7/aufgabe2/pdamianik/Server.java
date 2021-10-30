package at.ac.tgm.insy.sem7.aufgabe2.pdamianik;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import org.intellij.lang.annotations.Language;
import org.json.*;
import java.sql.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * INSY Webshop at.ac.tgm.insy.sem7.aufgabe2.pdamianik.Server
 */
public class Server {

    /**
     * Port to bind to for HTTP service
     */
    private static final int PORT = 8000;

    /**
     * Connect to the database
     * @throws IOException
     */
    Connection setupDB()  {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        Properties dbProps = new Properties();
        try {
            dbProps.load(new FileInputStream(rootPath + "db.properties"));
            return DriverManager.getConnection(dbProps.getProperty("url"), dbProps);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Startup the Webserver
     * @throws IOException
     */
    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/articles", new ArticlesHandler());
        server.createContext("/clients", new ClientsHandler());
        server.createContext("/placeOrder", new PlaceOrderHandler());
        server.createContext("/orders", new OrdersHandler());
        server.createContext("/", new IndexHandler());

        server.start();
    }


    public static void main(String[] args) throws Throwable {
        Server webshop = new Server();
        webshop.start();
        System.out.println("Webshop running at http://127.0.0.1:" + PORT);
    }


    /**
     * Handler for listing all articles
     */
    class ArticlesHandler implements HttpHandler {
        @Language("PostgreSQL")
        private static final String QUERY = "SELECT * FROM articles;";

        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = Util.queryToJSON(conn, QUERY);

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t,res.toString());
        }

    }

    /**
     * Handler for listing all clients
     */
    class ClientsHandler implements HttpHandler {
        @Language("PostgreSQL")
        private static final String QUERY = "SELECT * FROM clients;";

        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = Util.queryToJSON(conn, QUERY);

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t,res.toString());
        }

    }


    /**
     * Handler for listing all orders
     */
    class OrdersHandler implements HttpHandler {
        @Language("PostgreSQL")
        private static final String QUERY = "SELECT o.id AS id, c.name AS client, COUNT(ol.id) AS lines, SUM(a.price * ol.amount) AS price " +
                "FROM order_lines ol " +
                "INNER JOIN articles a on ol.article_id = a.id " +
                "INNER JOIN orders o on o.id = ol.order_id " +
                "INNER JOIN clients c on c.id = o.client_id " +
                "GROUP BY o.id, c.name " +
                "ORDER BY o.id;";

        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = Util.queryToJSON(conn, QUERY);

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t,res.toString());
        }

    }

   
    /**
     * Handler class to place an order
     */
    class PlaceOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {

            Connection conn = setupDB();
            Map <String,String> params  = queryToMap(t.getRequestURI().getQuery());

            int client_id = Integer.parseInt(params.get("client_id"));

            String response;
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id FROM orders ORDER BY id DESC LIMIT 1");
                rs.next();
                int order_id = rs.getInt("id") + 1;
                stmt.close();

                PreparedStatement ps;

                ps = conn.prepareStatement("INSERT INTO orders (id, client_id) VALUES (?, ?);");
                ps.setInt(1, order_id);
                ps.setInt(2, client_id);
                ps.executeUpdate();
                ps.close();

                for (int i = 1; i <= (params.size()-1) / 2; ++i ){
                    int article_id = Integer.parseInt(params.get("article_id_"+i));
                    int amount = Integer.parseInt(params.get("amount_"+i));

                    ps = conn.prepareStatement("SELECT amount FROM articles WHERE id = ?;");
                    ps.setInt(1, article_id);
                    rs = ps.executeQuery();
                    rs.next();
                    int available = rs.getInt(1);
                    rs.close();
                    ps.close();

                    if (available < amount)
                        throw new IllegalArgumentException(String.format("Not enough items of article #%d available", article_id));

                    ps = conn.prepareStatement("UPDATE articles SET amount = amount - ? WHERE id = ?;");
                    ps.setInt(1, amount);
                    ps.setInt(2, article_id);
                    ps.executeUpdate();
                    ps.close();

                    ps = conn.prepareStatement("INSERT INTO order_lines (article_id, order_id, amount) VALUES (?, ?, ?)");
                    ps.setInt(1, article_id);
                    ps.setInt(2, order_id);
                    ps.setInt(3, amount);
                    ps.executeUpdate();
                    ps.close();
                }

                t.getResponseHeaders().set("Content-Type", "application/json");
                response = String.format("{\"order_id\": %d}", order_id);
            } catch (IllegalArgumentException | SQLException iae) {
                iae.printStackTrace();
                response = String.format("{\"error\":\"%s\"}", iae.getMessage());
            }

            answerRequest(t, response);


        }
    }

    /**
     * Handler for listing static index page
     */
    class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<!doctype html>\n" +
                    "<html><head><title>INSY Webshop</title><link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/water.css@2/out/water.css\"></head>" +
                    "<body><h1>INSY Pseudo-Webshop</h1>" +
                    "<h2>Verf&uuml;gbare Endpoints:</h2><dl>"+
                    "<dt>Alle Artikel anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+ PORT +"/articles\">http://127.0.0.1:"+ PORT +"/articles</a></dd>"+
                    "<dt>Alle Bestellungen anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+ PORT +"/orders\">http://127.0.0.1:"+ PORT +"/orders</a></dd>"+
                    "<dt>Alle Kunden anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+ PORT +"/clients\">http://127.0.0.1:"+ PORT +"/clients</a></dd>"+
                    "<dt>Bestellung abschicken:</dt><dd><a href=\"http://127.0.0.1:"+ PORT +"/placeOrder?client_id=<client_id>&article_id_1=<article_id_1>&amount_1=<amount_1&article_id_2=<article_id_2>&amount_2=<amount_2>\">http://127.0.0.1:"+ PORT +"/placeOrder?client_id=&lt;client_id>&article_id_1=&lt;article_id_1>&amount_1=&lt;amount_1>&article_id_2=&lt;article_id_2>&amount_2=&lt;amount_2></a></dd>"+
                    "</dl></body></html>";

            answerRequest(t, response);
        }

    }


    /**
     * Helper function to send an answer given as a String back to the browser
     * @param t HttpExchange of the request
     * @param response Answer to send
     * @throws IOException
     */
    private void answerRequest(HttpExchange t, String response) throws IOException {
        byte[] payload = response.getBytes();
        t.sendResponseHeaders(200, payload.length);
        OutputStream os = t.getResponseBody();
        os.write(payload);
        os.close();
    }

    /**
     * Helper method to parse query parameters
     * @param query the url query to parse
     * @return all get parameters in the url query
     */
    public static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }

  
}
