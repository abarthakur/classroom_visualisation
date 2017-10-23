import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/* The main client class, which represents the GUI on the client end*/
public class ClientGui implements ActionListener{
	
	//top level container window
	JFrame mainWindow;
	//dailog box used to dispay errors/success/waiting
	JDialog mainDialog=null;
	
	//components of the login window that need to be accessed(areas to display errors,text fields, button)
	JLabel nameError, rollError, seatError;
	JTextField nameField , rollField,seatField;
	JButton signButton;
	
	String name;
	int roll;
	int seat;

	String address = null;
	int port;
	
	private static Socket socket =null;
    private static DataInputStream input= null;
  	private static DataOutputStream output = null;

	ClientGui(){
		mainWindow= new JFrame ("Login Portal");

		mainWindow.setMinimumSize(new Dimension(400,600));
		mainWindow.setMaximumSize(new Dimension(400,600));
		mainWindow.setResizable(false);

		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel logInPanel = new JPanel();
		buildLoginPanel(logInPanel);
		mainWindow.add(logInPanel);
		
		mainWindow.setVisible(true);
	}	

	//function to build the login panel with all components like fields, sign in button etc
	private void buildLoginPanel(JPanel panel){

		ImageIcon image = new ImageIcon("./client_images/login_image.jpg");
		//rescale image
		image = new ImageIcon ( image.getImage().getScaledInstance(400,300,Image.SCALE_DEFAULT));
                JLabel imageLabel = new JLabel("", image, JLabel.CENTER);
	
		/*String fonts[]=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();	
		for ( int i = 0; i < fonts.length; i++ ){
			System.out.println(fonts[i]);
		}*/

		Font labelFont = new Font("URW Gothic L",Font.BOLD, 15);
		Font errorFont = new Font("TlwgTypewriter",Font.PLAIN,10);
		//JLabels for holding heading of fields text 
		JLabel nameLabel = new JLabel("NAME");
		JLabel rollLabel = new JLabel("ROLL NO.");
		JLabel seatLabel= new JLabel("SEAT NO.");		

		nameLabel.setFont(labelFont);
		rollLabel.setFont(labelFont);
		seatLabel.setFont(labelFont);

		//JLabel for holding error text, initially empty, populated later
		nameError =new JLabel();
		nameError.setForeground(Color.red);
		rollError = new JLabel();
		rollError.setForeground(Color.red);
		seatError = new JLabel();
		seatError.setForeground(Color.red);

		//input fields
		nameField = new JTextField();
		rollField = new JTextField();
		seatField = new JTextField();

		//create login button				
		ImageIcon buttonImage = new ImageIcon("./client_images/login_button.png");
		buttonImage = new ImageIcon ( buttonImage.getImage().getScaledInstance(60,24,Image.SCALE_DEFAULT));
		signButton = new JButton (buttonImage);
		//register listener
		signButton.addActionListener(this);

		panel.setBackground(Color.white);

		//arrange all components in panel 

//		imageLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red),imageLabel.getBorder()));
		panel.add(imageLabel);
		
		Dimension defaultDim = new Dimension(400,20);
		Dimension errorDim = new Dimension (400,15);
		
		nameLabel.setPreferredSize(defaultDim);	
		panel.add(nameLabel);		

		nameError.setPreferredSize(errorDim);
		panel.add(nameError);

		nameField.setPreferredSize(defaultDim);
		panel.add(nameField);

		rollLabel.setPreferredSize(defaultDim);
		panel.add(rollLabel);

		rollError.setPreferredSize(errorDim);
		panel.add(rollError);

		rollField.setPreferredSize(defaultDim);
		panel.add(rollField);
	
		seatLabel.setPreferredSize(defaultDim);
		panel.add(seatLabel);
		
		seatError.setPreferredSize(errorDim);
		panel.add(seatError);

		seatField.setPreferredSize(defaultDim);
		panel.add(seatField);

		//put button within panel, since BoxLayout doesn't allow different alignments
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(signButton);
		//match alignment with JLabel component, which is reqd by BoxLayout
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		buttonPanel.setBackground(Color.white);
		panel.add(buttonPanel);

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

	}


	public void clearErrors(){
		nameError.setText("");
		rollError.setText("");
		seatError.setText("");
		nameError.repaint();
		rollError.repaint();
		seatError.repaint();
	}
	

	// called when sign in button is pressed
	public void actionPerformed (ActionEvent event){
		clearErrors();
		boolean wrong =false;
		name = nameField.getText();
		//check for invalid input and set error
		if (name.equals("")){
			nameError.setText("*Field required");
			wrong=true;
		}
		if (rollField.getText().equals("")){
			rollError.setText("*Field required");
			roll = 0;
			wrong=true;
		}
		else{

			try{
				roll = Integer.parseInt (rollField.getText());
			}
			catch(NumberFormatException e){
				rollError.setText("*Must be an integer");
				wrong=true;
			}
		}
		if (roll <0 ){
			rollError.setText("*Must be an integer");
			wrong=true;
		}


		if (seatField.getText().equals("")){
			seatError.setText("*Field required");
			wrong=true;
		}
		else{

			try{
				seat = Integer.parseInt (seatField.getText());
			}
			catch(NumberFormatException e){
				seatError.setText("*Must be an integer");
				wrong=true;
			}
		}

		//if no errors, start a thread to send login data
		if (!wrong){
			Thread t= new Thread(new ClientHelper(name,roll,seat));
			t.start();	
			buildWaitingDialog();
		}
		else{
			nameError.repaint();
			rollError.repaint();
			seatError.repaint();
		}

	}



	//runnable class that runs on separate thread to GUI thread and connects
	//to the server, sends client data, and reads back the status
	private class ClientHelper implements Runnable{
		String name;
		int roll;
		int seat;
		ClientHelper(String name , int roll, int seat){
			this.name=name;
			this.roll=roll;
			this.seat=seat;
		}


		public void run(){
			try {
				//connect to server, create input output streams
				socket=new Socket(address,port);
				System.out.println("Connected to " +address+":"+port);
				input = new DataInputStream(socket.getInputStream());
				output= new DataOutputStream(socket.getOutputStream());

				//write login data
				output.writeUTF(name);
				output.writeInt(roll);
				output.writeInt(seat);
				//read status
				int status=input.readInt();
				//dispatch runnable to Swing EventDispatchQueue which does the 
				//appropriate thing a/t status
				//IMPORTANT : never update GUI directly from a single thread,
				//all GUI related work should be done on the EventDispatchThread
				//which reads from the EventDispatchQueue
				SwingUtilities.invokeLater(new Runnable(){
					//anonymous runnable that calls the functions to make 
					//appropriate changes to GUI
					@Override
					public void run(){
						//status of 1 is success
						if (status==1){
							mainDialog.dispose();
							buildSuccessDialog();

						}
						//else error
						else{
							mainDialog.dispose();
							buildErrorDialog(status);
						}
					}
				});

				//release connection so that server is free to
				//attend different requests
				output.close();
				input.close();
				socket.close();
			}
			catch(IOException e){
				System.out.println(e);
			}
		}
	}


	//create a dialog box to inform user of successful login
	private void buildSuccessDialog(){
		mainDialog = new  JDialog(mainWindow,"Success!");
		//mainDialog.getContentPane().removeAll();
		JPanel p1 = new JPanel (new BorderLayout());
		p1.setBackground(Color.white);
		//success text
		JLabel lab = new JLabel("Login Successful!");
		lab.setFont( new Font("URW Gothic L",Font.BOLD, 15));
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		p1.add(lab, BorderLayout.NORTH);
		
		//success image
		ImageIcon img = new ImageIcon ("./client_images/success.png");
		img = new ImageIcon ( img.getImage().getScaledInstance(100,100,Image.SCALE_DEFAULT));
		JLabel imgLabel = new JLabel(img);
		p1.add(imgLabel,BorderLayout.CENTER);

		JButton ok = new JButton ("Ok");
		//pressing ok closes the application
		//register action listener (anonymous inner class is sufficient)
		ok.addActionListener (new ActionListener(){

						public void actionPerformed(ActionEvent e){
							//call GUI's close up method to gracefully release all
							//resources
							ClientGui.this.closeUp();
						}
					}
					);

		p1.add(ok,BorderLayout.SOUTH);

		//settings of the dialog box
		mainDialog.getContentPane().add(p1);
		mainDialog.setLocationRelativeTo(mainWindow);
		mainDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		mainDialog.setModal(true);
		mainDialog.setMinimumSize(new Dimension(300,200));
		mainDialog.setMaximumSize(new Dimension(300,200));
		mainDialog.setResizable(false);
		mainDialog.setVisible(true);
	
	}

	//create a dialog box with waiting animation, till server-client communication is completed
	//especially useful when server is busy
	private void buildWaitingDialog(){
		mainDialog = new JDialog(mainWindow,"Waiting for server...");
		JPanel  p1= new JPanel(new BorderLayout());
		p1.setBackground(Color.white);

		//waiting text
		JLabel lab = new JLabel("Please wait...");
		lab.setFont( new Font("URW Gothic L",Font.BOLD, 15));
		lab.setAlignmentX(Component.LEFT_ALIGNMENT);
		p1.add(lab, BorderLayout.NORTH);
		
		//waiting animation
		ImageIcon gif = new ImageIcon ("./client_images/load.gif");
		JLabel imgLabel = new JLabel(gif);
		p1.add(imgLabel,BorderLayout.CENTER);

		//abort button to close client application
		JButton abort = new JButton ("Abort");
		//register anonymous listener class that closes up application
		abort.addActionListener (new ActionListener(){
						public void actionPerformed(ActionEvent e){
							ClientGui.this.closeUp();
						}
					}
					);
		p1.add(abort,BorderLayout.SOUTH);

		//dialog settings
		mainDialog.getContentPane().add(p1);
		mainDialog.setLocationRelativeTo(mainWindow);
		mainDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		mainDialog.setModal(true);
		mainDialog.setMinimumSize(new Dimension(300,200));
		mainDialog.setMaximumSize(new Dimension(300,200));
		mainDialog.setResizable(false);
		mainDialog.setVisible(true);
		
	}

	//create error dialog in case server responded with error status
	private void buildErrorDialog(int status){
		mainDialog = new JDialog(mainWindow,"Error!");
		mainDialog.getContentPane().removeAll();
		JPanel p1 = new JPanel (new BorderLayout());
		JLabel error = new JLabel();
		error.setFont(new Font("TlwgTypewriter",Font.PLAIN,15));

		//display error msg
		if (status ==2){
			error.setText("<html>Oops! Your roll no doesn't seem to be in the database!</html>");
		}
		else if (status==3){
			error.setText("<html>Your seat no is not in range!</html>");
		}
		else if (status ==4){
			error.setText("<html>Oops! Someone is already sitting here!</html>");
		}
		p1.add(error, BorderLayout.CENTER);
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2,BoxLayout.X_AXIS));

		//button to try login again
		JButton tryAgain = new JButton("Try Again?");
		//listener that calls tryLoginAgain method on press
		tryAgain.addActionListener (new ActionListener(){
						public void actionPerformed(ActionEvent e){
							ClientGui.this.tryLoginAgain();
						}
					}
					);
		//abort button to close app
		JButton abort = new JButton ("Abort");
		abort.addActionListener (new ActionListener(){
						public void actionPerformed(ActionEvent e){
							ClientGui.this.closeUp();
						}
					}
				);


		tryAgain.setPreferredSize(new Dimension(150,20));
		abort.setPreferredSize(new Dimension(150,20));

		p2.add(Box.createHorizontalGlue());
		p2.add(tryAgain);
		p2.add(Box.createHorizontalGlue());
		p2.add(abort);
		p2.add(Box.createHorizontalGlue());

		p1.add(p2, BorderLayout.SOUTH);

		//dialog settings	
		mainDialog.getContentPane().add(p1);
		mainDialog.setLocationRelativeTo(mainWindow);
		mainDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		mainDialog.setModal(true);
		mainDialog.setMinimumSize(new Dimension(300,200));
		mainDialog.setMaximumSize(new Dimension(300,200));
		mainDialog.setResizable(false);
		mainDialog.setVisible(true);

	}

	//release socket, input stream, output stream , main mainWindow invisible, and exit
	public void closeUp(){
		try{
			if (socket!=null){
				socket.close();
			}
			if (input!=null){
				input.close();
			}
			if (output!=null){
				output.close();
			}
		}
		catch(IOException e){
			System.out.println(e);
		}
		mainDialog.dispose();
		mainDialog.dispose();
		System.exit(0);
	}

	
	//clear text fields, errors in login panel and close dialog
	public void tryLoginAgain(){
		nameField.setText("");
		rollField.setText("");
		seatField.setText("");
		nameField.repaint();
		seatField.repaint();
		rollField.repaint();
		clearErrors();
		mainDialog.dispose();		
	}


	//read command line params, and start the gui.
	public static void main(String[] args){
		ClientGui gui = new ClientGui();
		String address = args[0];
		gui.address=address;
		int port = Integer.parseInt(args[1]);
		gui.port = port;
		//unable to connect?
		return ; 

	}
	

}
	
