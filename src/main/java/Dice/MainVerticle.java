package Dice;

import java.util.Arrays;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;

public class MainVerticle extends AbstractVerticle{
    private Pool client;

     @Override
     public void start(Promise<Void> startPromise) throws Exception {
         // TODO Auto-generated method stub
         // do the sql database connection


// serevr connections:
     Router router =Router.router(vertx);
      router.route().handler(BodyHandler.create());

      // server create 
      HttpServer server = vertx.createHttpServer();
      server.requestHandler(router).listen(8070,arr->{
        if(arr.succeeded()){
            startPromise.complete();
            System.out.println("server started ");
        }

        else {
            startPromise.fail(arr.cause());
        }
      });


MySQLConnectOptions mySQLConnectOptions = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("localhost")
  .setDatabase("my_dice_db1")
  .setUser("root")
  .setPassword("sandy@2001");

// Pool options
PoolOptions poolOptions = new PoolOptions()
  .setMaxSize(5);

  client =Pool.pool(vertx,mySQLConnectOptions,poolOptions);
  checkconnection();


  router.get("/users").handler(context -> {
    client.preparedQuery("SELECT * FROM  my_users").execute(ar -> {
        if (ar.succeeded()) {
            JsonArray users = new JsonArray();
            RowSet<Row> rows = ar.result();
            for (Row row : rows) {
                JsonObject user = new JsonObject();
                user.put("name", row.getString("name"))
                    .put("password", row.getString("password"));

                users.add(user);
            }////////////////////////////////////////
            context.request().response().putHeader("content-type", "application/json").end("Users " + users.encodePrettily());
        } else {
            context.response().end("Error : " + ar.cause());
        }
    });
});


router.post("/add").handler(context->{
    JsonObject user = context.getBodyAsJson();
    String name = user.getString("name");
    String password = user.getString("password");

    if (isPalindromicSubstring(name, password)) {
      client.preparedQuery("INSERT INTO my_users(name, password) VALUES(?, ?)").execute(Tuple.of(name, password), res -> {
        if (res.succeeded()) {
          context.response().end("User Entered Successfully");
        } else {
          System.out.println("Error in saving user : " + res.cause().getMessage());
        }
      });
    } else {
      context.response().end("Password should be palindromic substring of name");
    }
    
});



     }


    

     private void checkconnection() {
        client.getConnection(ar -> {
          if (ar.succeeded()) {
            SqlConnection connection = ar.result();
            connection.query("SELECT 1").execute(result -> {
              if (result.succeeded()) {
                System.out.println("Database connection is successful.");
              } else {
                System.err.println("Database connection failed: " + result.cause().getMessage());
              }
              connection.close();
            });
          } else {
            System.err.println("Failed to get connection from pool: " + ar.cause());
          }
        });
      };     


      private boolean isPalindrome(String str, int i, int j) {
        while (i <= j) {
          if (str.charAt(i) != str.charAt(j)) {
            return false;
          }
          i++;
          j--;
        }
        return true;
      }
    
      private boolean isPalindromicSubstring(String name, String password) {
        for(int i=0; i<name.length(); i++){
          for(int j=i+1; j<name.length(); j++){
            String substr = name.substring(i, j);
            if(isPalindrome(substr, 0, substr.length()-1) && password.equals(substr)){
              return true;
            }
          }
        }
        return false;
      }
    
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }
    
}
