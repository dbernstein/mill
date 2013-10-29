/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.credentials.simpledb;

import java.util.LinkedList;
import java.util.List;

import org.duracloud.mill.credentials.AccountCredentials;
import org.duracloud.mill.credentials.AccountCredentialsNotFoundException;
import org.duracloud.mill.credentials.CredentialRepo;
import org.duracloud.mill.credentials.ProviderCredentials;
import org.duracloud.storage.domain.StorageProviderType;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;

/**
 * A simpledb-based implementation of the <code>CredentialsRepo</code>
 * @author Daniel Bernstein 
 *         Date: Oct 29, 2013
 */
public class SimpleDBCredentialsRepo implements CredentialRepo {
    private AmazonSimpleDBClient client;

    /**
     * @param client
     */
    public SimpleDBCredentialsRepo(AmazonSimpleDBClient client) {
        this.client = client;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.duracloud.mill.credentials.CredentialRepo#
     * getAccoundCredentialsBySubdomain(java.lang.String)
     */
    @Override
    public AccountCredentials getAccoundCredentialsBySubdomain(String subdomain)
            throws AccountCredentialsNotFoundException {
        //get server details id
        SelectResult result = this.client.select(new SelectRequest("select SERVER_DETAILS_ID from DURACLOUD_ACCOUNTS where SUBDOMAIN = '"+subdomain+"'"));
        List<Item> items = result.getItems();
        
        if(items.size() == 0){
            throw new AccountCredentialsNotFoundException("subdomain \""+ subdomain + "\" not found.");
        }

        String serverDetailsId = items.get(0).getAttributes().get(0).getValue();
        
        //get server details
        String serverDetailsDomain = "DURACLOUD_SERVER_DETAILS";
        GetAttributesResult getResult = this.client.getAttributes(new GetAttributesRequest(serverDetailsDomain, serverDetailsId));
        List<ProviderCredentials> creds = new LinkedList<ProviderCredentials>();
        
        //get primary providers
        String primaryId = getValue(getResult, "PRIMARY_STORAGE_PROVIDER_ACCOUNT_ID", serverDetailsDomain);
        creds.add(getProviderCredentials(primaryId));
        //parse secondary ids
        String secondaryIdsStr = getValue(getResult, "SECONDARY_STORAGE_PROVIDER_ACCOUNT_IDS", serverDetailsDomain);

        String[] secondaryIds =  secondaryIdsStr.trim().split(",");
        //for each secondary get providers
        for(String secondaryId : secondaryIds){
            creds.add(getProviderCredentials(secondaryId));
        }
        //build account credentials object.
        AccountCredentials accountCreds = new AccountCredentials();
        accountCreds.setSProviderCredentials(creds);
        accountCreds.setSubDomain(subdomain);
        return accountCreds;
    }
    

    /**
     * @param primaryId
     * @return
     */
    private ProviderCredentials getProviderCredentials(String id) {
        String domain = "DURACLOUD_STORAGE_PROVIDER_ACCOUNTS";
        GetAttributesResult result = this.client.getAttributes(new GetAttributesRequest(domain, id));
        String username = getValue(result, "USERNAME", domain);
        String password = getValue(result, "PASSWORD", domain);
        StorageProviderType providerType = StorageProviderType.valueOf(getValue(result, "PROVIDER_TYPE", domain));
        return new ProviderCredentials(id, username, password, providerType);
    }

    private String getValue(GetAttributesResult getResult,
            String attributeName, String domain) {
        List<Attribute> attrs = getResult.getAttributes();
        for (Attribute attr : attrs) {
            if (attr.getName().equals(attributeName)) {
                return attr.getValue();
            }
        }

        throw new RuntimeException("Not attribute named " + attributeName
                + " exists in result: domain=" + domain);
    }

}