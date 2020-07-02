
import java.io.*;
import java.net.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ChatClient extends Application {

    //initialize input and output streams
    private DataInputStream reader = null;
    private DataOutputStream writer = null;
    private Socket socket;
    private RegisterationForm registrationForm;
    private CreateLoginPage loginPage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primary) {

        //connect to the server
        try {
            socket = new Socket("localhost", 8000);
            //create input and output streams
            reader = new DataInputStream(socket.getInputStream());
            writer = new DataOutputStream(socket.getOutputStream());
            
            //instantiate the registration form
            registrationForm = new RegisterationForm(writer, reader, loginPage);
            loginPage = new CreateLoginPage(writer, registrationForm);
            
            //instantiate the chat room 
            ChatRoom chatRoom = new ChatRoom();
            
            //handle user inputs
            chatRoom.getTf().setOnAction(e
                    -> userInputs(chatRoom.getTf().getText(), chatRoom.getTf()));
            //handle exit requests
            chatRoom.getExit().setOnAction(e -> exitChatRoom(chatRoom));
            
            //create a thread that handle inputs of the server
            ManageServerInputs inputs = new ManageServerInputs(chatRoom, 
                    registrationForm, reader, loginPage);
            inputs.start();
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }

    }

    public void userInputs(String str, TextField textField) {
        textField.clear();
        try {
            writer.writeUTF(str);
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }

    }

    public void exitChatRoom(ChatRoom chatRoom) {
        try {
            String esc = "exit#";
            writer.writeUTF(esc);
            writer.close();
            reader.close();
            chatRoom.getStage().close();
            socket.close();
            
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }

    }
}
class CreateLoginPage {
    private Stage primary;
    private Text exception;
    
    public CreateLoginPage(DataOutputStream output, RegisterationForm registrationForm) {
        final double WIDTH2 = 400;
        final double HEIGHT2 = 150;
        primary = new Stage();
        //create the ccomponents
        Label usrName = new Label("User name");
        TextField userName = new TextField();
        Label pass = new Label("Password");
        PasswordField password = new PasswordField();
        Text register = new Text("Sign up");
        Text reminder = new Text("Forgot password?");
        Button submit = new Button("Submit");
        exception = new Text();

        //create the grid and add components to it
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setAlignment(Pos.CENTER);
        
        BorderPane mainPane = new BorderPane();

        grid.add(usrName, 0, 0);
        grid.add(userName, 1, 0);
        grid.add(pass, 0, 1);
        grid.add(password, 1, 1);
        grid.add(submit, 2, 1);
        grid.add(register, 0, 2);
        grid.add(reminder, 2, 2);
        mainPane.setTop(grid);
        mainPane.setCenter(exception);
        //add the grid to the scene and place the scene on the stage
        Scene scene2 = new Scene(mainPane, WIDTH2, HEIGHT2);

        primary.setTitle("Prompt User");
        primary.setScene(scene2);
        primary.show();

        //create the event handlers
        register.setOnMouseEntered(e -> {
            register.setFill(Color.BLUE);
        });
        register.setOnMouseExited(e -> {
            register.setFill(Color.BLACK);
        });
        reminder.setOnMouseEntered(e -> {
            reminder.setFill(Color.BLUE);
        });
        reminder.setOnMouseExited(e -> {
            reminder.setFill(Color.BLACK);
        });
       
        //create the registration form
        register.setOnMouseClicked(registrationForm);
        
        //send log in requestes
        submit.setOnAction(e -> {
            String message = "login?," + userName.getText().trim()
                    + "," + password.getText().trim();
            //send the login request to the server
            try {
                output.writeUTF(message);
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        });
    }
    //getters
    public Stage getStage(){
        return primary;
    }
    public Text getText(){
        return exception;
    }

}

class ManageServerInputs extends Thread {

    private ChatRoom chatRoom;
    private RegisterationForm registrationForm;
    private DataInputStream reader;
    private CreateLoginPage loginPage;

    //constructor
    public ManageServerInputs(ChatRoom chatRoom, RegisterationForm registrationForm,
            DataInputStream reader, CreateLoginPage loginPage) {
        this.chatRoom = chatRoom;
        this.registrationForm = registrationForm;
        this.reader = reader;
        this.loginPage = loginPage;
    }

    @Override
    public void run() {

        try {
            while (true) {

                String str = reader.readUTF();

                if (str.contains("#log")) {
                    String [] data = new String[1];
                    data = str.split(",");
                    data[1] = data[1] + " ON\n";
                    chatRoom.getUsersArea().appendText(data[1]);
                    Platform.runLater(() -> {
                        loginPage.getStage().hide();
                        chatRoom.getStage().show();
                    });
                     
                } else if (str.contains("offline#")) {
                    String name = str.substring(0, str.indexOf(":")).trim();
                    String hold = chatRoom.getUsersArea().getText();
                    int index = hold.indexOf(name);
                    chatRoom.getUsersArea().deleteText(index, index + name.length() + 4);
                } else if (str.contains("exception")) {
                    Platform.runLater(() ->{
                    registrationForm.getStage().show();
                    registrationForm.getText().setFill(Color.FIREBRICK);
                    registrationForm.getText().setText("a user with this "
                            + "username already exists, please choose a "
                            + "diffrent name");
                    });

                } else if(str.contains("online#")){
                    //extract the name then place it on the users area
                    String name = str.substring(0, str.indexOf(":")).trim();
                    name = name + " ON\n";
                    chatRoom.getUsersArea().appendText(name);
                } else if (str.contains("loginException")){
                    String exception = "sorry, that didn't work!, please try again";
                    Platform.runLater(() -> {
                        loginPage.getText().setFill(Color.FIREBRICK);
                        loginPage.getText().setText(exception);
                    });
                
                }else{
                    chatRoom.getTa().appendText(str + "\n");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}

class RegisterationForm implements EventHandler<MouseEvent> {

    final int WIDTH_FORM = 600;
    final int HEIGHT_FORM = 200;
    private DataInputStream in;
    private DataOutputStream out;
    private CreateLoginPage loginPage;
    private Stage form = new Stage();

    //create the components
    private Text exception = new Text();
    private TextField first = new TextField();
    private TextField last = new TextField();
    private CheckBox male = new CheckBox("M");
    private CheckBox female = new CheckBox("F");
    private TextField user = new TextField();
    private PasswordField pass = new PasswordField();
    private PasswordField pass2 = new PasswordField();
    private Button submit = new Button("submit");

    public RegisterationForm(DataOutputStream out, DataInputStream in, CreateLoginPage loginPage) {
        this.in = in;
        this.out = out;
        this.loginPage = loginPage;
    }

    //getters
    public Text getText() {
        return exception;
    }

    @Override
    public void handle(MouseEvent event) {
        
        Label firstName = new Label("First name");
        Label lastName = new Label("Last Name");
        Label gender = new Label("Gender");
        Label userNameForm = new Label("User name");
        Label password = new Label("Password");
        Label password2 = new Label("re-enter password");

        HBox button = new HBox();
        button.setAlignment(Pos.CENTER_RIGHT);
        button.getChildren().add(submit);

        HBox genContainer = new HBox();
        genContainer.setAlignment(Pos.CENTER);
        genContainer.setSpacing(10);
        genContainer.getChildren().addAll(gender, male, female);

        BorderPane pane = new BorderPane();
        //create a grid and add components to it
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setAlignment(Pos.CENTER);

        grid.add(firstName, 0, 0);
        grid.add(lastName, 2, 0);
        grid.add(first, 1, 0);
        grid.add(last, 3, 0);
        grid.add(userNameForm, 0, 1);
        grid.add(user, 1, 1);
        grid.add(gender, 2, 1);
        grid.add(genContainer, 3, 1);
        grid.add(password, 0, 2);
        grid.add(pass, 1, 2);
        grid.add(password2, 2, 2);
        grid.add(pass2, 3, 2);
        grid.add(button, 3, 3);

        pane.setTop(grid);
        pane.setCenter(exception);
        Scene scene = new Scene(pane, WIDTH_FORM, HEIGHT_FORM);
        //set the scene on the stage
        form.setTitle("Registration form");
        form.setScene(scene);
        form.show();
        //submit the form
        submit.setOnAction(e -> submitInformation());
    }

    public void submitInformation() {
        String password1 = pass.getText();
        String password2 = pass2.getText();
        if (!(password1.equals(password2))) {
            exception.setFill(Color.FIREBRICK);
            exception.setText("password didn't match");
        } else {
            String firstName = first.getText();
            String lastName = last.getText();

            String userName = user.getText();
            if (firstName.equalsIgnoreCase("")
                    || lastName.equalsIgnoreCase("")
                    || userName.equalsIgnoreCase("")) {
                exception.setFill(Color.FIREBRICK);
                exception.setText("please fill all the fields");
            } else {
                if (male.isSelected()) {
                    String stream = "register," + userName + "," + firstName + "," + lastName + "," + "M," + password1;
                    try {
                        
                        out.writeUTF(stream);
                        form.hide();
                        
                    } catch (IOException ex) {
                        exception.setFill(Color.FIREBRICK);
                        exception.setText("Server failed to recieve data");

                    }
                } else if (female.isSelected()) {
                    String stream = "register," + userName + "," + firstName + "," + lastName + "," + "M," + password1;
                    try {
                        out.writeUTF(stream);
                        form.hide();
                    } catch (IOException ex) {
                        exception.setFill(Color.FIREBRICK);
                        exception.setText("Server failed to recieve data");
                    }
                } else {
                    exception.setFill(Color.FIREBRICK);
                    exception.setText("Please select the gender");
                }
            }

        }
    }
    public Stage getStage(){
        return form;
    }

}

class ChatRoom {

    final double WIDTH = 400;
    final double HEIGHT = 300;
    private Scene scene;
    private TextArea ta;
    private TextArea usersArea;
    private TextField tf;
    private Button exit;
    private Stage primary;

    //constructor
    public ChatRoom() {
        //create the chat output area
        primary = new Stage();
        ta = new TextArea();
        ta.setEditable(false);
        ScrollPane pane = new ScrollPane(ta);
        pane.setPrefSize(WIDTH * 0.7, HEIGHT * 0.7);
        pane.setLayoutX(0);

        //create the online/offline users area
        usersArea = new TextArea();
        usersArea.setEditable(false);
        ScrollPane pane2 = new ScrollPane(usersArea);
        pane2.setPrefSize(WIDTH * 0.3, HEIGHT * 0.7);
        pane2.setLayoutX(WIDTH * 0.7);

        Pane topContainer = new Pane();
        topContainer.getChildren().addAll(pane, pane2);

        //create the chat input area
        tf = new TextField();
        tf.setPrefSize(WIDTH * 0.7, HEIGHT * 0.3);
        tf.setPromptText("write your text here");
        //create an exit button
        exit = new Button("EXIT");
        exit.setPrefSize(WIDTH * 0.3, HEIGHT * 0.3);
        exit.setLayoutX(WIDTH * 0.7);
        exit.setStyle("-fx-font-weight:bold;");

        Pane bottomContainer = new Pane();
        bottomContainer.getChildren().addAll(tf, exit);

        BorderPane borderPane = new BorderPane();

        borderPane.setTop(topContainer);
        borderPane.setBottom(bottomContainer);

        scene = new Scene(borderPane, WIDTH, HEIGHT);
        primary.setTitle("Welcome to the chat room");
        primary.setScene(scene);
    }

    //getters

    public Button getExit() {
        return exit;
    }

    public TextArea getTa() {
        return ta;
    }

    public TextArea getUsersArea() {
        return usersArea;
    }

    public TextField getTf() {
        return tf;
    }
    public Stage getStage(){
        return primary;
    }
}
