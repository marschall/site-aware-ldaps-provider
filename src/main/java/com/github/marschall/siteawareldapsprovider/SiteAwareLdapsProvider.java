package com.github.marschall.siteawareldapsprovider;

import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.spi.LdapDnsProvider;
import javax.naming.ldap.spi.LdapDnsProviderResult;

public final class SiteAwareLdapsProvider extends LdapDnsProvider {
  
  // https://nitschinger.at/Bootstrapping-from-DNS-SRV-records-in-Java/
  // https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-dns.html
  // https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-ldap.html
  
  
  // ldap:///dc=example,dc=com
  // _ldap._tcp.example.com

  @Override
  public Optional<LdapDnsProviderResult> lookupEndpoints(String url, Map<?, ?> env) throws NamingException {
    // ldaps:///dc=example,dc=com
    return Optional.empty();
  }
  
  static String toServiceName(String url) {
    return url;
  }
  
  private static void lookupService(String serviceName) throws NamingException {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
    env.put(Context.PROVIDER_URL, "dns:");
    DirContext context = new InitialDirContext(env);
    Attributes attribues = context.getAttributes(serviceName, new String[] { "SRV" });

    NamingEnumeration<?> servers = attribues.get("srv").getAll();
    try {
//    Set<DnsRecord> sortedRecords = new TreeSet<DnsRecord>();
      while (servers.hasMore()) {
        String server = (String) servers.next();
        String[] splitted = server.split(" ");
        String priority = splitted[0];
        String weight = splitted[1];
        String port = splitted[2];
        String host = splitted[3];
        System.out.println(server);
//      DnsRecord record = DnsRecord.fromString(server);
//      sortedRecords.add(record);
      }
    } finally {
      servers.close();
    }
  }
  
  public static void main(String[] args) throws NamingException {
    String serviceName = "_xmpp-server._tcp.gmail.com";
    lookupService(serviceName);
  }

}
