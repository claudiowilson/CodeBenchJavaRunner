package CodeBench;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
            throws ClassNotFoundException, SQLException, IOException {
        //Connect to the postgres database
        Class.forName("org.postgresql.Driver");
        Connection connection = null;
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres", "postgres",
                "yoloswag");
        connection.setAutoCommit(false);

        //Get all the code for the current submission
        String sql = "SELECT * FROM codebench.code WHERE submission_id=" + submissionID + ";";
        Statement statement = connection.createStatement();
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

            CodeFile codeFile = new CodeFile(fileName, submissionCode);
            codeFiles.add(codeFile);
        }

        createJavaFile(codeFiles, "sub_" + submissionID);

            /*if (submissionCode != null) {
                File filePath = new File("programs" + File.separator
                        + generateFileName());
                filePath.mkdirs();

                String fileName = filePath + File.separator + "program.java";
                File codeFile = new File(fileName);
                codeFile.createNewFile();
                PrintWriter writer = new PrintWriter(fileName, "UTF-8");
                writer.println("import java.io.*;");
                writer.println("import java.util.*;");
                writer.println("");
                writer.println("public class program {");
                writer.println(submissionCode.replace("\r", ""));
                writer.println("}");
                writer.close();

                // ////////////////////////////////////////////////////////////////
                // Get the Question Input and Output
                int questionID = result.getInt("question");
                Statement input_statement = connection.createStatement();
                ResultSet input_result = input_statement
                        .executeQuery("SELECT * FROM codebench.question where question_id="
                                + questionID + ";");

                String correct_output = "";
                while (input_result.next()) {
                    correct_output = input_result.getString("output");
                    String inputFileName = filePath + File.separator
                            + "input.txt";
                    File f = new File(inputFileName);
                    f.createNewFile();
                    writer = new PrintWriter(inputFileName, "UTF-8");
                    writer.print(input_result.getString("input"));
                    writer.close();
                    break;
                }

                Manager.runProgram(fileName, submissionID, connection, correct_output);

                codeFile.delete();
                File compiledFile = new File("programs" + File.separator
                        + fileName.replace(".java", ".class"));
                if (compiledFile.exists())
                    compiledFile.delete();
                File inputFile = new File(filePath + File.separator
                        + "input.txt");
                if (inputFile.exists())
                    inputFile.delete();
                filePath.delete();
            }*/
    }

    private static void createJavaFile(ArrayList<CodeFile> codeFiles, String directoryName) {
        //Create the folder
        File directory = new File(directoryName);
        directory.mkdirs();
        //Clean out all the files already in the directory
        try {
            FileUtils.cleanDirectory(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Look at each code file
        for (CodeFile codeFile : codeFiles) {
            //Create the file
            String fileName = directoryName + File.separator + codeFile.getFileName();
            File file = new File(fileName);
            try {
                file.createNewFile();

                //Add the code to the file
                PrintWriter writer = new PrintWriter(fileName, "UTF-8");
                writer.print(codeFile.getCode());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String generateFileName() {
        String ans = "";
        for (int i = 0; i < 15; i++) {
            ans += (char) ((int) ((Math.random() * 26)) + 97);
        }
        return ans;
    }

    public static void main(String args[]) throws Exception {
        getData(0);
        /*ConnectionFactory factory = new ConnectionFactory();
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
        }*/
    }
}
