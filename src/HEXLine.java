public class HEXLine {
	public static final String EOL="\n";

	public String line;
		
	public String start;
	public String bc;
	public int byteCount;
	public String adr;
	public int startAddress;
	public String rec;
	public int recInt;
	public String data;
	public String chk;
	public int chkInt;
	public int chkComputed;
	public String end;
		
	public StringBuffer errors;
		
	private int pos;
		
	// parse methods will return true if parsing may continue, or false if parsing cannot continue
	
	private boolean parseStart() {
		pos=line.indexOf(":");
		if(pos<0) {
			start=line;
			errors.append("No data");
			return false;
		}
		start=line.substring(0,pos+1);
		pos++;
		return true;
	}
	
	private boolean parseByteCount() {
		try{ bc=line.substring(pos,pos+2); }catch(StringIndexOutOfBoundsException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Line too short; cannot read byteCount");
			end=line.substring(pos);
			return false;
		}
		try { byteCount=Integer.parseInt(bc,16); }catch(NumberFormatException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Invalid characters in byteCount");
			end=line.substring(pos+2);
			return false;
		}
		pos+=2;
		return true;
	}
	
	private boolean parseAddress() {
		try{ adr=line.substring(pos,pos+4); }catch(StringIndexOutOfBoundsException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Line too short; cannot read address");
			end=line.substring(pos);
			return false;
		}
		try { startAddress=Integer.parseInt(adr,16); }catch(NumberFormatException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Invalid characters in address");
		}
		pos+=4;
		return true;
	}

	private boolean parseRecordType() {
		try{ rec=line.substring(pos,pos+2); }catch(StringIndexOutOfBoundsException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Line too short; cannot read recordType");
			end=line.substring(pos);
			return false;
		}
		try { recInt=Integer.parseInt(rec,16); }catch(NumberFormatException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Invalid characters in recordType");
		}
		pos+=2;
		return true;
	}

	private boolean parseData() {
		try{ data=line.substring(pos,pos+byteCount*2); }catch(StringIndexOutOfBoundsException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Line too short; cannot read data");
			end=line.substring(pos);
			return false;
		}
		pos+=byteCount*2;

		// compute checksum and validate bytes
		chkComputed=0;
		int currentByteInt=0;
		for(int i=0;i<byteCount;i++) {
			String currentByte=data.substring(2*i,2*i+2);
			try { currentByteInt=Integer.parseInt(currentByte,16); }catch(NumberFormatException ex) {
				if(errors.length()>0)errors.append(EOL);
				errors.append("Invalid characters in data");
				chkComputed=-1;
				break;
			}
			chkComputed+=currentByteInt;
			chkComputed&=0xFF;
		}
		
		return true;
	}

	private boolean parseChecksum() {
		try{ chk=line.substring(pos,pos+2); }catch(StringIndexOutOfBoundsException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Line too short; cannot read checksum");
			end=line.substring(pos);
			return false;
		}
		try { chkInt=Integer.parseInt(chk,16); }catch(NumberFormatException ex) {
			if(errors.length()>0)errors.append(EOL);
			errors.append("Invalid characters in checksum");
		}
		pos+=2;
		return true;
	}

	private boolean parseEnd() {
		end=line.substring(pos);
		return true;
	}
	
	private void finishChecksumComputation() {
		chkComputed+=byteCount;
		chkComputed&=0xFF;
		
		chkComputed+=startAddress&0xFF;
		chkComputed&=0xFF;
		
		chkComputed+=(startAddress>>8);
		chkComputed&=0xFF;
		
		chkComputed+=recInt;
		chkComputed&=0xFF;
		
		chkComputed^=0xFF;
		chkComputed+=1;
		chkComputed&=0xFF;
	}

	public HEXLine(String line) {
		this.line=line;
		start="";
		bc="";
		byteCount=0;
		adr="";
		startAddress=0;
		rec="";
		recInt=-1;
		data="";
		chk="";
		end="";
		errors=new StringBuffer();
		
		if(!parseStart())return ;
		if(!parseByteCount())return ;
		if(!parseAddress())return ;
		if(!parseRecordType())return ;
		if(!parseData())return ;
		if(!parseChecksum())return ;
		parseEnd();
		finishChecksumComputation();
		if(chkComputed!=chkInt) {
			if(errors.length()>0)errors.append(EOL);
			errors.append(String.format("Invalid checksum value; should be %02X (%d)",chkComputed,chkComputed));
		}
		
	}
}
