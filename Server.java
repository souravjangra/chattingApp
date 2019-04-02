import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Color;

public class Server
{
	private int port;
	private List<User> Clients;
	private ServerSocket server;

	public static void main(String[] args )throws java.net.BindException, IOException
	{
		new Server(8080).run();
	}
	public Server(int port)
	{
		this.port=port;
		this.Clients=new ArrayList<User>();
	}
	public void run() throws IOException
	{
		// one thread for accepting the new clients
		server = new ServerSocket(port)
		{
			protected void finalize() throws IOException
			{
				this.close();
			}
		};
		System.out.println("POrt 8080 is on now");

		while(true)
		{
			//accepts a new client
			Socket client =server.accept();
			//get the handle name
			String handle =(new Scanner (client.getInputStream() )).nextLine();
			handle =handle .replace(",","");// used for serialisation
			handle =handle.replace(" ","_");// for serialisation
			System.out.println("New Client : \" "+handle +"\"\n\t Host : "+client.getInetAddress().getHostAddress());

			// create a new user
			User newUser =new User(client,handle);

			// add the newUser to list of all the client 
			this.Clients.add(newUser);

			//welcome msg
			newUser.getOutStream().println("Welcome and this day would be the best of your life");

			// create a new thread for newUser incomming message 
			new Thread(new UserHandler(this,newUser)).start();
		}
	}

	//delete a user from list
	public void removeUser(User user)
	{
		this.Clients.remove(user);
	}

	//send incomming msg to all users, used when having a chatting in a group 
	public void broadcastMessage(String msg,User userSender)
	{
		for(User client :this.Clients)
		{
			client.getOutStream().println(userSender.toString()+"<span> : "+ msg+"</span>");
		}
	}

	//send list of client to all users
	// this would be used when a person wants to 
	public void braodcastAllUsers()
	{
		for(User client :this.Clients)
		{
			client.getOutStream().println(this.Clients);
		}
	}

	// send a message to user privately 
	public void sendMessageToUser(String msg,User userSender,String user)
	{
		boolean find=false;
		for(User client :this.Clients)
		{
			if(client.getNickname().equals(user) && client!= userSender)
			{
				find=true;
				userSender.getOutStream().println(userSender.toString()+" -> "+client.toString() + ": "+msg);
				client.getOutStream().println("Private talks "+userSender.toString()+"<span> :"+msg+"</span>");
			}
		}
		if(!find)
			userSender.getOutStream().println(userSender.toString()+" ->(<br> no one ! </b> ): "+msg);
	}
}
// another class handling the User
class UserHandler implements Runnable
{
	private Server server;
	private User user;

	public UserHandler(Server server, User user)
	{
		this.server=server;
		this.user=user;
		this.server.braodcastAllUsers();
	}

	public void run()
	{
		String message;
		// when there is a new message ,broadcast to all
		Scanner sc=new Scanner(this.user.getInputStream());
		while(sc.hasNextLine())
		{
			message=sc.nextLine();

			// add some replace stuff to make things better to replace moji 

			// gestion des messages private
			if(message.charAt(0)=='@')
			{
				if(message.contains(" "))
				{
					System.out.println("private msg : "+message);
					int firstSpace=message.indexOf(" ");
					String userPrivate =message.substring(1,firstSpace);
					server.sendMessageToUser(message.substring(firstSpace+1,message.length()),user,userPrivate);
				}
			}
			// gestion du changement
			else if(message.charAt(0)=='#')
			{
				user.changeColor(message);
				//update color for all other users
				this.server.braodcastAllUsers();
			}
			else
			{
				//braodcast the message
				server.broadcastMessage(message,user);
			}
		}
			// end of thread 
			server.removeUser(user);
			this.server.braodcastAllUsers();
			sc.close();
		}
	}

class User
{
	public static int nbUser=0;
	private int userId;
	private PrintStream streamOut;
	private InputStream streamIn;
	private String handle,color;
	private Socket client;

	public User(Socket client,String name) throws IOException
	{
		this.streamOut=new PrintStream(client.getOutputStream());
		this.streamIn=client.getInputStream();
		this.client=client;
		this.handle=handle;
		this.userId=nbUser;
		this.color=ColorInt.getColor(this.userId);
		nbUser++;
	}

	//change color user
	public void changeColor(String hexColor)
	{
		//check if it is valid hexcolor
		Pattern colorPattern=Pattern.compile("#([0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})");
		Matcher m =colorPattern.matcher(hexColor);
		if(m.matches())
		{
		    Color c = Color.decode(hexColor);
			// if color is too bright then donot change the color
		    double luma = 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue(); // per ITU-R BT.709
      		if (luma > 160) 
      		{
		        this.getOutStream().println("<b>Color Too Bright</b>");
		        return;
      		}
	      this.color = hexColor;
	      this.getOutStream().println("<b>Color changed successfully</b> " + this.toString());
	      return;
		}
		this.getOutStream().println("<b> Failed to change the color write the correct code ");
	}

	  // getters
	  public PrintStream getOutStream()
	  {
	    return this.streamOut;
	  }

	  public InputStream getInputStream()
	  {
	    return this.streamIn;
	  }

	  public String getNickname()
	  {
	    return this.handle;
	  }

	  // print user with his color, overloading a tostring function in class declaration
	  public String toString()
	  {
	    return "<u><span style='color:"+ this.color
	      +"'>" + this.getNickname() + "</span></u>";
	  }
}

class ColorInt
{
	public static String[] mcolors=
	{
		"#3079ab", // dark blue
        "#e15258", // red
        "#f9845b", // orange
        "#7d669e", // purple
        "#53bbb4", // aqua
        "#51b46d", // green
        "#e0ab18", // mustard
        "#f092b0", // pink
        "#e8d174", // yellow
        "#e39e54", // orange
        "#d64d4d", // red
        "#4d7358", // green
	};
	public static String getColor (int i)
	{
		return mcolors[i%mcolors.length];
	}
}