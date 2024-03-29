package CodeBench;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
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
    public static String getData(int submissionID)
            throws ClassNotFoundException, SQLException, IOException, InterruptedException {
        System.out.print("Processing submission " + submissionID + ": ");

        //Connect to the postgres database
        Class.forName("org.postgresql.Driver");
        Connection connection = null;
        connection = DriverManager.getConnection(
                "jdbc:postgresql://107.170.12.71:5432/codebench", "postgres",
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
            System.out.println("Invalid language");
            statement.executeUpdate("UPDATE codebench.submission SET errors = 'Unrecognized language' WHERE " +
                    "submission_id=" + submissionID + ";");
            connection.commit();
            return "Invalid language";
        }

        //Get the question from the ID
        resultSet = statement.executeQuery("SELECT * FROM codebench.question WHERE question_id=" + questionID + ";");
        resultSet.next();
        //Get the correct output for the question
        String correctOutput[] = resultSet.getString("output").split("\\r?\\n");
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
            return "Unrecognized Language";
        }

        codeRunner.createFiles();
        String compileResult = codeRunner.compileFiles();

        //Update the table with the result of the compilation
        if (compileResult == null) {
            statement.executeUpdate("UPDATE codebench.submission SET errors = null WHERE " +
                    "submission_id=" + submissionID + ";");
            connection.commit();
        }
        else {
            System.out.println("UPDATE codebench.submission SET errors = $err_msg$" + compileResult + "$err_msg$ WHERE " +
                    "submission_id=" + submissionID + ";");
            statement.executeUpdate("UPDATE codebench.submission SET errors = $err_msg$" + compileResult + "$err_msg$ WHERE " +
                    "submission_id=" + submissionID + ";");
            connection.commit();

            //If the program failed to compile, don't even bother trying to run it
            statement.executeUpdate("UPDATE codebench.submission SET time_taken = Interval '0 milliseconds'" + "WHERE submission_id=" + submissionID + ";");
            connection.commit();
            return compileResult;
        }

        String outputResult = "";

        //Get the output for the program
        long startTime = System.currentTimeMillis();
        int outputIndex = 0;
        boolean correctSubmission = true;
        StringBuilder builder = new StringBuilder();
        for(String args : input) {
        	String output = codeRunner.runProgram(args.split(" "));
        	output = output.trim();
            outputResult += "TESTING: Input = " + correctOutput[outputIndex] + "\n======================\n" + output;
        	if(!output.equals(correctOutput[outputIndex])) {
        		correctSubmission = false;
        		builder.append("Incorrect output! Your output was: " +
                    output + " but the correct output is: " + correctOutput[outputIndex] +"\n");
                outputResult += "\n[Incorrect output!]";
        	} else {
                outputResult += "\n[Correct output!]";
            }
        	outputIndex++;
        }
        
        long millisecondsToRun = System.currentTimeMillis() - startTime;
        statement.executeUpdate("UPDATE codebench.submission SET time_taken = Interval '" + millisecondsToRun + " " +
                "milliseconds' " + "WHERE submission_id=" + submissionID + ";");

        if (correctSubmission) {
            System.out.println("Correct submission");
            statement.executeUpdate("UPDATE codebench.submission SET errors = null WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        else {
            System.out.println("Incorrect submission");
            statement.executeUpdate("UPDATE codebench.submission SET errors = '" +
                    builder.toString() + "' WHERE " +
                    "submission_id=" + submissionID + ";");
        }
        connection.commit();
        statement.close();
        connection.close();

        return outputResult + "\n======================\n[Total runtime: " + millisecondsToRun + "]";
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
            case "c":
                return "c";
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
        channel.queueBind(queueName, EXCHANGE_NAME, "java");

        channel.basicQos(1);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        System.out.println("Running version 1.0!");
        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            
            BasicProperties props = delivery.getProperties();
            BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                .correlationId(props.getCorrelationId())  // Correlate request with response
                .build();

            String message = new String(delivery.getBody());
            try {
                int submissionId = Integer.parseInt(message);
                String result = getData(submissionId);
                channel.basicPublish("", props.getReplyTo(), (AMQP.BasicProperties) replyProps, result.getBytes());
            } catch (ClassNotFoundException | SQLException | IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}
