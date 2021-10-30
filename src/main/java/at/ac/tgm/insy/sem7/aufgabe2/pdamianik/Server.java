package at.ac.tgm.insy.sem7.aufgabe2.pdamianik;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

import at.ac.tgm.insy.sem7.aufgabe2.pdamianik.model.Article;
import at.ac.tgm.insy.sem7.aufgabe2.pdamianik.model.Client;
import at.ac.tgm.insy.sem7.aufgabe2.pdamianik.model.Order;
import at.ac.tgm.insy.sem7.aufgabe2.pdamianik.model.OrderLine;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.intellij.lang.annotations.Language;
import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.persistence.TypedQuery;

/**
 * INSY Webshop at.ac.tgm.insy.sem7.aufgabe2.pdamianik.Server
 */
public class Server {

    /**
     * Port to bind to for HTTP service
     */
    private static final int PORT = 8000;

    private SessionFactory sessionFactory;

    /**
     * Connect to the database
     */
    Session setupDB() throws IOException {
        if (sessionFactory == null) {
            String propertiesFile = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("db.properties")).getPath();

            Properties properties = new Properties();

            try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(propertiesFile))) {
                properties.load(stream);
            }

            StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml")
                    .applySetting("hibernate.connection.url", properties.getProperty("url"))
                    .applySetting("hibernate.connection.username", properties.getProperty("user"))
                    .applySetting("hibernate.connection.password", properties.getProperty("password"))
                    .build();
            Metadata metadata = new MetadataSources(ssr).getMetadataBuilder().build();
            sessionFactory = metadata.getSessionFactoryBuilder().build();
        }
        return sessionFactory.openSession();
    }

    /**
     * Startup the Webserver
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
        @Language("HQL")
        private static final String QUERY= "FROM Article";

        @Override
        public void handle(HttpExchange t) throws IOException {
            Session conn = setupDB();
            TypedQuery<Article> query = conn.createQuery(QUERY);

            List<Article> articles = query.getResultList();
            JSONArray res = new JSONArray(articles);

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t,res.toString());
            conn.close();
        }
    }

    /**
     * Handler for listing all clients
     */
    class ClientsHandler implements HttpHandler {
        @Language("HQL")
        private static final String QUERY = "FROM Client";

        @Override
        public void handle(HttpExchange t) throws IOException {
            Session conn = setupDB();
            TypedQuery<Client> query = conn.createQuery(QUERY);

            List<Client> clients = query.getResultList();
            JSONArray res = new JSONArray(clients);

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t,res.toString());
            conn.close();
        }
    }


    /**
     * Handler for listing all orders
     */
    class OrdersHandler implements HttpHandler {
        @Language("HQL")
        private static final String QUERY = "SELECT " +
                "o.id AS id, " +
                "c.name AS client_name, " +
                "COUNT(ol) AS article_count, " +
                "SUM(a.price * ol.amount) AS price " +
                "FROM Order AS o " +
                "INNER JOIN o.client AS c " +
                "INNER JOIN o.orderLines AS ol " +
                "JOIN ol.article AS a " +
                "GROUP BY o.id, c.name";

        @Override
        public void handle(HttpExchange t) throws IOException {
            Session conn = setupDB();

            Query query = conn.createQuery(QUERY)
                    .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
            JSONArray res = new JSONArray(query.getResultList());

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, res.toString());
            conn.close();
        }
    }

   
    /**
     * Handler class to place an order
     */
    class PlaceOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().set("Content-Type", "application/json");

            Session conn = setupDB();
            Map<String,String> params  = queryToMap(t.getRequestURI().getQuery());

            int client_id = Integer.parseInt(params.get("client_id"));
            TypedQuery<Client> query = conn.createQuery("FROM Client C WHERE C.id = " + client_id);
            Client client = query.getSingleResult();

            String response;
            try {
                conn.beginTransaction();

                Order order = new Order();

                order.setClient(client);
                order.setCreatedAt(new Date());
                order.setOrderLines(new HashSet<>());

                for (int i = 1; i <= (params.size()-1) / 2; ++i ){
                    int article_id = Integer.parseInt(params.get("article_id_"+i));
                    int amount = Integer.parseInt(params.get("amount_"+i));

                    TypedQuery<Article> query1 = conn.createQuery("FROM Article WHERE id = " + article_id);
                    Article article = query1.getSingleResult();
                    int available = article.getAmountAvailable();

                    if (available < amount)
                        throw new IllegalArgumentException(String.format("Not enough items of article #%d available", article_id));

                    article.setAmountAvailable(available - amount);
                    conn.update(article);

                    OrderLine orderLine = new OrderLine();
                    orderLine.setArticle(article);
                    orderLine.setAmount(amount);
                    conn.save(orderLine);
                    order.getOrderLines().add(orderLine);
                }
                conn.save(order);
                conn.getTransaction().commit();

                response = String.format("{\"order_id\": %d}", order.getId());
                answerRequest(t, response);
            } catch (Exception e) {
                conn.getTransaction().rollback();
                e.printStackTrace();
                response = String.format("{\"error\":\"%s\"}", e.getMessage());
                t.sendResponseHeaders(400, response.getBytes().length);
                OutputStream responseStream = t.getResponseBody();
                responseStream.write(response.getBytes(StandardCharsets.UTF_8));
                responseStream.close();
            }
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
