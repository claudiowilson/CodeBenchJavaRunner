package CodeBench;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class DatabaseManager {
    private static final String EXCHANGE_NAME = "codebench";

    /**
     * Get all the data corresponding to the submission with the given ID.
     *
     * @param submissionID
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public static void getData(int submissionID)
            throws ClassNotFoundException, SQLException, IOException, InterruptedException {
        System.out.print("Processing submission " + submissionID + ": ");

        //Connect to the postgres database
        Class.forName("org.postgresql.Driver");
        Connection connection = null;
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres", "postgres",
                "yoloswag");
        connection.setAutoCommit(false);

        //--------Get the correct output from the table
        //Get the current submission
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM codebench.submission WHERE submission_id=" +
                submissionID + ";");
        resultSet.next();
        //Get the ID for the question this submission is for
        String questionID = resultSet.getString("question");
        //Get the language for the submission
        String language = resultSet.getString("language");
        String extension = getExtensionFromLanguage(language);

        //Ensure the language is recognized
        if (extension == null) {
            statement.executeUpdate("UPDATE codebench.submission SET errors = 'Unrecognized language' WHERE " +
                    "submission_id=" + submissionID + ";");
            connection.commit();
            return;
        }

        //Get the question from the ID
        resultSet = statement.executeQuery("SELECT * FROM codebench.question WHERE question_id=" + questionID + ";");
        resultSet.next();
        //Get the correct output for the question
        String correctOutput = resultSet.getString("output");
        //Get the input for the question
        String input[] = resultSet.getString("input").split("\\r?\\n");

        //Get all the code for the current submission
        String sql = "SELECT * FROM codebench.code WHERE submission_id=" + submissionID + " ORDER BY code_id;";
        ResultSet result = statement.executeQuery(sql);

        //The list of code files that correspond to the current submission
        ArrayList<CodeFile> codeFiles = new ArrayList<>();

        //Add each code block to the list
        while (result.next()) {
            String submissionCode = result.getString("code");
            String fileName = result.getString("class_name");

            //Skip over invalid submissions
            if (submissionCode == null || fileName == null) {
                System.out.println("Invalid submission");
                continue;
            }

            CodeFile codeFile = new CodeFile(fileName, submissionCode, extension);
            codeFiles.add(codeFile);
        }

        //Create the .java and .class files
        String directory = "sub_" + submissionID;
        CodeRunner codeRunner = CodeRunner.createRunner(language, codeFiles, directory);

        //If the CodeRunner is null, the language was not recognized and we cannot continue
        if (codeRunner == null) {
            statement.executeUpdate("UPDATE codebench.submission SET errors = 'Unrecognized language' WHERE " +
                    "submission_id=" + submissionID + ";");
            connection.commit();
            return;
        }

        codeRunner.createFiles();
        String compileResult = codeRunner.compileFiles();

        //Update the table with the result of the compilation
        if (compileResult == null) {
            statement.executeUpdate("UPDATE codebench.submission SET errors = null WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        else {
            statement.executeUpdate("UPDATE codebench.submission SET errors = '" + compileResult + "' WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        connection.commit();

        //Get the output for the program
        String output = codeRunner.runProgram(input);

        //Trim the output to get rid of trailing new line characters
        output = output.trim();

        if (output.equals(correctOutput)) {
            System.out.println("Correct submission");
            statement.executeUpdate("UPDATE codebench.submission SET errors = null WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        else {
            System.out.println("Incorrect submission");
            statement.executeUpdate("UPDATE codebench.submission SET errors = 'Incorrect output! Your output was: " +
                    output + " but the correct output is: " + correctOutput + "' WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        connection.commit();
        statement.close();
        connection.close();
    }

    /**
     * Returns the file extension of the given language. If the language is not recognized, null is returned.
     *
     * @param language
     *
     * @return
     */
    private static String getExtensionFromLanguage(String language) {
        if (language == null)
            return null;

        switch (language.trim().toLowerCase()) {
            case "java":
                return "java";
            case "python":
                return "py";
            default:
                return null;
        }
    }

    public static void main(String args[]) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri("amqp://guest:guest@107.170.12.71:5672");
        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "#");
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        System.out.println("running!");
        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            try {
                int submissionId = Integer.parseInt(message);
                getData(submissionId);
            } catch (ClassNotFoundException | SQLException | IOException | NumberFormatException e) {
                e.printStackTrace();
            }
            System.out.println(message);
        }
    }
}
