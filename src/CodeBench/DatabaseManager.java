package CodeBench;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.Connection RabbitConnection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

public class DatabaseManager {
		private static final String EXCHANGE_NAME = "codebench";
		
	    public static void getData(int submissionID)
	            throws ClassNotFoundException, SQLException, IOException {
	    Class.forName("org.postgresql.Driver");
	    Connection connection = null;
	    // System.out.print("Connecting...");
	    connection = DriverManager.getConnection(
	                    "jdbc:postgresql://localhost:5432/codebench", "postgres",
	                    "yoloswag");
	    connection.setAutoCommit(false);
	    String sql = "SELECT * FROM codebench.submission;";
	    // System.out.println("Done!");
	    Statement statement = connection.createStatement();
	    ResultSet result = statement.executeQuery(sql);
	
	    while (result.next()) {
	            String submissionCode = result.getString("code");
	            // System.out.println(submissionCode);
	            if (submissionCode != null) {
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
	            }
	    }
	    // System.out.println("statement built");
	    result.close();
	    statement.close();
	    connection.close();
	
	    System.out.println(submissionID);
	    // CommunicationManager.sendMessage(submissionID);
	}
	
	private static String generateFileName() {
	    String ans = "";
	    for (int i = 0; i < 15; i++) {
	            ans += (char) ((int) ((Math.random() * 26)) + 97);
	    }
	    return ans;
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
		while(true) {
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
