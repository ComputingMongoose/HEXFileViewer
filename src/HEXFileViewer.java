

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

@SuppressWarnings("serial")
public class HEXFileViewer  extends JFrame{
	public static final String NL="<br/>\n";

	public static void main(String[] args) {
		if(args.length!=1) {
			System.out.println("HEXFileViewer <HEX_file>");
			return ;
		}
		
		System.setProperty("sun.java2d.uiScale", "1.0");
		javax.swing.plaf.FontUIResource myFont=new javax.swing.plaf.FontUIResource("Arial",Font.BOLD,16);
		UIManager.put("Label.font", myFont);
		UIManager.put("TextField.font", myFont);
		UIManager.put("TextPane.font", myFont);
		UIManager.put("TextArea.font", myFont);
		
		SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
            	try {
					new HEXFileViewer(args[0]);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException(e);
				}
            }
        });
		
	}
	
	public void loadHEX(String fname) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(fname),Charset.forName("UTF8")));
		StringBuffer text=new StringBuffer();
		text.append("<span style=\"font-family:Consolas; font-size:16px;\">");
		hexLines.clear();
		int minAddress=-1;
		int maxAddress=-1;
		int executionAddress=0;
		for(String line = reader.readLine();line!=null;line=reader.readLine()) {
			if(hexLines.size()>0)text.append(NL);
			
			HEXLine hl=new HEXLine(line);
			hexLines.add(hl);
			
			text.append(String.format("<span style=\"background-color:%s; color:%s;\">%s</span>",
					(hl.errors.length()==0)?("#eeebeb"):("#ff0000"),
					(hl.errors.length()==0)?("#787878"):("#403d3d"),
					String.format("%4d",hexLines.size()).replace(" ", "&nbsp;") 
			));
			
			text.append(hl.start);

			if(hl.errors.length()==0) {
				if(hl.recInt==0) {
					if(minAddress==-1 || hl.startAddress<minAddress)minAddress=hl.startAddress;
					if(maxAddress<hl.startAddress)maxAddress=hl.startAddress;
				}else if(hl.recInt==1) {
					executionAddress=hl.startAddress;
				}
			}
			
			text.append("<span style=\"background-color:#CCFFCC; color:black;\">"+hl.bc+"</span>");
			text.append("<span style=\"background-color:#CCCCFF; color:black;\">"+hl.adr+"</span>");
			text.append("<span style=\"background-color:#FFCCCC; color:black;\">"+hl.rec+"</span>");
			text.append("<span style=\"background-color:#CCFFFF; color:black;\">"+hl.data+"</span>");
			text.append("<span style=\"background-color:#CCCCCC; color:black;\">"+hl.chk+"</span>");
			text.append(hl.end);
		}
		reader.close();
		text.append("</span>");
		
		fileViewer.setText(text.toString());
		fileViewer.setCaretPosition(0);
		loaded=true;
		
		hexStart=new int[hexLines.size()];
		hexEnd=new int[hexLines.size()];
		
		for(int i=0;i<hexLines.size();i++) {
			int prev=-1;
			if(i>0)prev=hexEnd[i-1];
			prev++;
			hexStart[i]=prev;
			hexEnd[i]=prev+hexLines.get(i).line.length()+4;
			if(i==0)hexEnd[i]++;
			//System.out.println(hexLines.get(i).length()+": " + hexStart[i]+" -> "+hexEnd[i]);
		}
		
		propsNumberOfRecords.setText(hexLines.size()+"");
		propsMinAddress.setText(String.format("%X (%d)",minAddress,minAddress));
		propsMaxAddress.setText(String.format("%X (%d)",maxAddress,maxAddress));
		propsExecutionAddress.setText(String.format("%X (%d)",executionAddress,executionAddress));
		
		propsRec.setVisible(false);

		propsExtern.revalidate();
	}
	
	
	public JTextPane fileViewer;
	private boolean loaded=false;
	private ArrayList<HEXLine> hexLines=new ArrayList<>(2000);
	private int[] hexStart;
	private int[] hexEnd;
	
	private JTextField propsNumberOfRecords=new JTextField();
	private JTextField propsMinAddress=new JTextField();
	private JTextField propsMaxAddress=new JTextField();
	private JTextField propsExecutionAddress=new JTextField();
	private JTextField propsRecNumber=new JTextField();
	private JTextField propsRecByteCount=new JTextField();
	private JTextField propsRecAddress=new JTextField();
	private JTextField propsRecType=new JTextField();
	private JTextPane propsRecData=new JTextPane();
	private JTextField propsRecChk=new JTextField();
	private JTextArea propsRecErrors=new JTextArea(5,30);
	
	private JPanel propsRec;
	
	private JPanel propsExtern;
	
	public HEXFileViewer(String fname) throws IOException {
		super("HEX File Viewer");
		setSize(1400,763);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		//Cursor cur = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		//this.setCursor(cur);
		//this.addMouseListener(new FrontWindowMouseListener());
		
		Container contentPane = this.getContentPane();
        SpringLayout layout = new SpringLayout();
        contentPane.setLayout(layout);
        fileViewer=new JTextPane();
        //fileViewer.setFont(new Font("Consolas", Font.PLAIN, 50));
        //Font font = new Font("Serif", Font.ITALIC, 18);
        //fileViewer.setFont(font);        
        fileViewer.setContentType("text/html");
        fileViewer.setEditable(false);
        fileViewer.setHighlighter(null);
        fileViewer.addCaretListener(new CaretListener() {

			@Override
			public void caretUpdate(CaretEvent arg0) {
				if(!loaded)return;
				//System.out.println("caret event");
				//System.out.println(arg0.getDot());
				
				int pos=arg0.getDot();
				for(int i=0;i<hexStart.length;i++) {
					if(pos>=hexStart[i] && pos<=hexEnd[i]) {
						HEXLine hl=hexLines.get(i);
						propsRecNumber.setText(String.format("%d",i+1));
						propsRecByteCount.setText(String.format("%s (%d)",hl.bc,hl.byteCount));
						propsRecAddress.setText(String.format("%s (%d)",hl.adr,hl.startAddress));
						propsRecType.setText(String.format("%s - %s", hl.rec,
								(hl.recInt==0)?("Data"):(
								(hl.recInt==1)?("End of File"):(
								(hl.recInt==2)?("Extended Segment Address"):(
								(hl.recInt==3)?("Start Segment Address"):(
								(hl.recInt==4)?("Extended Linear Address"):(
								(hl.recInt==5)?("Start Linear Address"):("Unknown"
								))))))
								));
						propsRecData.setText(hl.data);
						propsRecChk.setText(hl.chk);
						propsRecErrors.setText(hl.errors.toString());
						
						propsRec.setVisible(true);
						
						propsRec.revalidate();
						
						//System.out.println(hexLines.get(i));
						break;
					}
				}
			}
        });
        JPanel noWrapPanel = new JPanel( new BorderLayout() );
        noWrapPanel.add( fileViewer );        
        JScrollPane jsp=new JScrollPane(noWrapPanel);
        contentPane.add(jsp);

        propsExtern=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel props=new JPanel();
        propsExtern.add(props);
        props.setLayout(new BoxLayout(props,BoxLayout.PAGE_AXIS));
        contentPane.add(propsExtern);	
        this.setMinimumSize(new Dimension(900,500));
        Border blackline = BorderFactory.createLineBorder(Color.black);
        propsExtern.setBorder(blackline);
        
        
        JPanel current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        props.add(current);
        JLabel l=new JLabel("Records");//, JLabel.TRAILING);
        l.setLabelFor(propsNumberOfRecords);
        current.add(l);
        current.add(propsNumberOfRecords);
        //propsNumberOfRecords.setMinimumSize(minimumSize);
        propsNumberOfRecords.setText("0");
        propsNumberOfRecords.setEditable(false);
        
        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        props.add(current);
        l=new JLabel("Min Address");//, JLabel.TRAILING);
        l.setLabelFor(propsMinAddress);
        current.add(l);
        current.add(propsMinAddress);
        propsMinAddress.setText("0");
        propsMinAddress.setEditable(false);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        props.add(current);
        l=new JLabel("Max Address");//, JLabel.TRAILING);
        l.setLabelFor(propsMaxAddress);
        current.add(l);
        current.add(propsMaxAddress);
        propsMaxAddress.setText("0");
        propsMaxAddress.setEditable(false);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        props.add(current);
        l=new JLabel("Execution Address");//, JLabel.TRAILING);
        l.setLabelFor(propsExecutionAddress);
        current.add(l);
        current.add(propsExecutionAddress);
        propsExecutionAddress.setText("0");
        propsExecutionAddress.setEditable(false);

        propsRec=new JPanel();
		propsRec.setVisible(false);
        props.add(propsRec);
        propsRec.setLayout(new BoxLayout(propsRec,BoxLayout.PAGE_AXIS));

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("");//, JLabel.TRAILING);
        current.add(l);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("Current Record");//, JLabel.TRAILING);
        current.add(l);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("Record Number");//, JLabel.TRAILING);
        l.setLabelFor(propsRecNumber);
        current.add(l);
        current.add(propsRecNumber);
        propsRecNumber.setText("0");
        propsRecNumber.setEditable(false);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("Byte Count");//, JLabel.TRAILING);
        l.setLabelFor(propsRecByteCount);
        current.add(l);
        current.add(propsRecByteCount);
        propsRecByteCount.setText("0");
        propsRecByteCount.setEditable(false);
        
        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("Address");//, JLabel.TRAILING);
        l.setLabelFor(propsRecAddress);
        current.add(l);
        current.add(propsRecAddress);
        propsRecAddress.setText("0");
        propsRecAddress.setEditable(false);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("Type");//, JLabel.TRAILING);
        l.setLabelFor(propsRecType);
        current.add(l);
        current.add(propsRecType);
        propsRecType.setText("0");
        propsRecType.setEditable(false);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("Data");//, JLabel.TRAILING);
        l.setLabelFor(propsRecData);
        current.add(l);
        current.add(propsRecData);
        propsRecData.setText("0");
        propsRecData.setEditable(false);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("Checksum");//, JLabel.TRAILING);
        l.setLabelFor(propsRecChk);
        current.add(l);
        current.add(propsRecChk);
        propsRecChk.setText("0");
        propsRecChk.setEditable(false);

        current=new JPanel(new FlowLayout(FlowLayout.LEFT));
        propsRec.add(current);
        l=new JLabel("Errors");//, JLabel.TRAILING);
        l.setLabelFor(propsRecErrors);
        current.add(l);
        JScrollPane scrollPane = new JScrollPane(propsRecErrors); 
        current.add(scrollPane);
        propsRecErrors.setText("");
        propsRecErrors.setEditable(false);

        //fileViewer.setText("test text");

        layout.putConstraint(SpringLayout.EAST,  propsExtern, -5, SpringLayout.EAST,  contentPane);
        layout.putConstraint(SpringLayout.NORTH, propsExtern, 5, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.SOUTH,  propsExtern, -5, SpringLayout.SOUTH,  contentPane);
                
        layout.putConstraint(SpringLayout.WEST,  jsp, 5, SpringLayout.WEST,  contentPane);
        layout.putConstraint(SpringLayout.NORTH, jsp, 5, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.SOUTH, jsp, -5, SpringLayout.SOUTH, contentPane);
        layout.putConstraint(SpringLayout.EAST,  jsp, -5, SpringLayout.WEST,  propsExtern);
        
        layout.getConstraints(propsExtern).setWidth(Spring.constant(500,500,500));        
        
		setVisible(true);
		
		loadHEX(fname);
	}
	

}
