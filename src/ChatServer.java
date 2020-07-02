
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane;

public class ChatServer extends Application {

    private Connection connection;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primary) {

        //initialize database
        initializeDB();

        TextArea ta = new TextArea();
        ScrollPane pane = new ScrollPane(ta);
        Scene scene = new Scene(pane, 350, 300);

        primary.setTitle("Chat Server");
        primary.setScene(scene);
        primary.show();

        Server server = new Server(ta, connection);
        server.start();
    }

    public void initializeDB() {
        try {
            //load the postgresql driver
            Class.forName("org.postgresql.Driver");

            //connect to the database . this line should be adjusted according to your database configurationS
            connection = DriverManager.getConnection("jdbc:postgresql://localhost/database", "postgres", "password");

        } catch (ClassNotFoundException ex) {
            System.out.println(ex.toString());
        } catch (SQLException ex) {
            System.out.println(ex.toString());
        }
    }
}

class Server extends Thread {

    private TextArea ta;
    private Connection connection;
    private Set<ServerAgent> list;

    public Server(TextArea ta, Connection connection) {
        this.ta = ta;
        this.connection = connection;
    }

    @Override
    public void run() {
        //create a server socket
        list = new HashSet();
        try {
            ServerSocket serverSocket = new ServerSocket(8000);
            Platform.runLater(() -> {
                ta.appendText("Server started at " + new Date() + "\n");
            });
            //create a thread for each client
            while (true) {
                Socket socket = serverSocket.accept();
                Platform.runLater(() -> {
                    ta.appendText("New client has connected\n");
                });
                ServerAgent agent = new ServerAgent(this, socket, connection);
                list.add(agent);
                agent.start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void broadCast(ServerAgent agent, String message) throws IOException {
        for (ServerAgent a : list) {
            if (!(message.contains("offline#"))) {
                if (a.getOnlineStatus()) {
                    a.sendMessage(message);
                }
            } else {
                if (!(a.equals(agent))) {
                    a.sendMessage(message);
                }
            }
        }
    }

    public void notifyMe(ServerAgent agent) throws IOException {
        for (ServerAgent a : list) {
            String msg = ": is online#";
            if (a.getOnlineStatus()) {
                if (!(a.equals(agent))) {
                    msg = a.getUserName() + msg;
                    agent.sendMessage(msg);
                }
            }

        }
    }

    //show the current agent the online agents
    public void appendToBoard(String message) {
        ta.appendText(message);
    }

    public void removeAgent(ServerAgent agent) {
        list.remove(agent);
    }
}
//create the agent that should handle communications with the client

class ServerAgent extends Thread {

    private Connection connection;
    private Socket socket;
    private DataOutputStream writer;
    private String message;
    private Server server;
    private boolean onlineStatus;

    public ServerAgent(Server server, Socket socket, Connection connection) {
        this.socket = socket;
        this.server = server;
        this.connection = connection;
        message = "";
        onlineStatus = false;

    }

    public String getUserName() {
        return message;
    }

    public Boolean getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(boolean status) {
        onlineStatus = status;
    }

    @Override
    public void run() {

        try {

            //create input and output streams
            DataInputStream fromClient = new DataInputStream(socket.getInputStream());
            writer = new DataOutputStream(socket.getOutputStream());
            
            
            //handle registeration requestes
            registerNewUser(fromClient);

            //handle log in requests
            logTheUserIn(fromClient);


            String clientMessage;
            do {

                clientMessage = fromClient.readUTF();
                if (!(clientMessage.contains("exit#"))) {
                    server.appendToBoard(message + ": " + clientMessage + "\n");
                    clientMessage = message + ": " + clientMessage;
                    server.broadCast(this, clientMessage);
                }

            } while (!(clientMessage.contains("exit#")));

            //notify users that this user has disconnected
            String off = message + ": is offline#\n";

            socket.close();
            server.broadCast(this, off);
            server.removeAgent(this);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void registerNewUser(DataInputStream fromClient) throws IOException {
        boolean isRegistred = false;
        do {
            String str = fromClient.readUTF();
            //register the user
            if (str.contains("register")) {
                //extract the data out of the stream
                String[] data = new String[6];
                data = str.split(",");
                //insert user information to the database
                String query = "INSERT INTO users(username, first_name, last_name,"
                        + "gender, password) VALUES(? , ?, ? ,?, ?)";
                try {
                    PreparedStatement statement = connection.prepareStatement(query);
                    statement.setString(1, data[1]);
                    statement.setString(2, data[2]);
                    statement.setString(3, data[3]);
                    statement.setString(4, data[4]);
                    statement.setString(5, data[5]);
                    statement.execute();
                    System.out.println("succefully registred a new user");
                    isRegistred = true;
                } catch (SQLException ex) {
                    String exception = "exception";
                    writer.writeUTF(exception);
                    isRegistred = false;
                }
            } else {
                isRegistred = true;
            }
        } while (isRegistred == false);
    }

    public void logTheUserIn(DataInputStream fromClient) {
        boolean isLogedIn = false;
        do {
            try {
                String str = fromClient.readUTF();
                
                if (str.contains("login?")) {
                    String[] data = new String[3];
                    data = str.split(",");
                    // create a request statement
                    String query = "SELECT username FROM users WHERE "
                            + "username = ? AND password = ?";
                    try {
                        PreparedStatement preparedStatement = connection.prepareStatement(query);
                        preparedStatement.setString(1, data[1]);
                        preparedStatement.setString(2, data[2]);
                        ResultSet set = preparedStatement.executeQuery();
                        if (set.next()) {
                            String username = set.getString(1);
                            // login the user to the chat room
                            String loginUser = "#log the user in," + username;
                            writer.writeUTF(loginUser);
                            server.appendToBoard(username + " has connected \n");
                            //report to other users that this user is online
                            String toUser = username + ": is online#";
                            server.broadCast(this, toUser);
                            message = username;
                            this.onlineStatus = true;
                            //report the online users to this user
                            server.notifyMe(this);
                            isLogedIn = true;
                        } else {
                            String exception = "loginException";
                            writer.writeUTF(exception);
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        } while (isLogedIn == false);
    }

    public void sendMessage(String message) throws IOException {
        writer.writeUTF(message);
    }
}
