package org.dcm4chex.archive.dcm.qrscp;

import java.sql.SQLException;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.jdbc.QueryCmd;

public class BlockedFindScp extends FindScp {

	public BlockedFindScp(QueryRetrieveScpService service) {
		super(service, false);
	}

	protected Dataset getDataset(QueryCmd queryCmd) throws SQLException {
		Dataset parent = DcmObjectFactory.getInstance().newDataset();
		DcmElement sq = parent.putSQ(Tags.DirectoryRecordSeq);
		sq.addItem(queryCmd.getDataset());
		for (int i = 1, n = service.getMaxBlockedFindRSP();
		i <= n && queryCmd.next(); ++i) {
			sq.addItem(queryCmd.getDataset());
		}
		return parent;
	}						
	
}
