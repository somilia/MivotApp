package vollt_tuning;

import java.io.IOException;
import java.io.OutputStream;

import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.data.TableIterator;
import tap.formatter.OutputFormat;

public class MangoFormat implements OutputFormat {

	CustomVOTableFormat votFmt;
	public MangoFormat(ServiceConnection service) {
		votFmt=new CustomVOTableFormat(service);
	}
	
	@Override
	public String getMimeType() {
		// TODO Auto-generated method stub
		return "application/mango";
	}

	@Override
	public String getShortMimeType() {
		// TODO Auto-generated method stub
		return "mango";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return votFmt.getDescription();
	}

	@Override
	public String getFileExtension() {
		// TODO Auto-generated method stub
		return votFmt.getFileExtension();
	}

	@Override
	public void writeResult(TableIterator result, OutputStream output, TAPExecutionReport execReport, Thread thread)
			throws TAPException, IOException, InterruptedException {
		// TODO Auto-generated method stub
		votFmt.writeResult(result, output, execReport, thread);
	}

}
