package org.dcm4chex.rid.mbean;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.dcm4chee.docstore.BaseDocument;
import org.dcm4chee.docstore.DocumentStore;
import org.dcm4chee.docstore.Feature;

public class RIDStorageDelegate {

    private static final String STORE_DOMAIN = "RID";

    private static final Set<Feature> retrieveFeatures = new HashSet<Feature>();
    static {
        retrieveFeatures.add( new Feature("RID_RETRIEVE", "RID Retrieve") );
    }

    private static RIDStorageDelegate singleton;

    private DocumentStore store;

    private RIDStorageDelegate() {
        store = DocumentStore.getInstance(STORE_DOMAIN, STORE_DOMAIN);
        store.setRetrieveFeatures(retrieveFeatures);
    }

    public static RIDStorageDelegate getInstance() {
        if ( singleton == null ) 
            singleton= new RIDStorageDelegate();
        return singleton;
    }

    public BaseDocument getDocument(String docUid, String mime) {
        return store.getDocument(docUid, mime);
    }
    public BaseDocument createDocument(String docUid, String mime) throws IOException {
        return store.createDocument(docUid, mime);
    }

    public boolean removeDocument(String docUid) {
        return store.deleteDocument(docUid);
    }

}
