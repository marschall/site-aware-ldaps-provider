package com.github.marschall.siteawareldapsprovider;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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

  private static final int LDAP_PORT = 389;
  private static final int LDAPS_PORT = 636;
  private static final String[] SERVICE_NAMES = new String[] { "SRV" };

  // https://nitschinger.at/Bootstrapping-from-DNS-SRV-records-in-Java/
  // https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-dns.html
  // https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-ldap.html
  // https://bugs.openjdk.java.net/browse/JDK-8192975



  @Override
  public Optional<LdapDnsProviderResult> lookupEndpoints(String url, Map<?, ?> env) throws NamingException {
    // ldaps:///dc=example,dc=com
    return Optional.empty();
  }

  static String toServiceName(String url) throws NamingException {
    // ldap:///dc=example,dc=com
    // _ldap._tcp.example.com
    if (!url.startsWith("ldaps:///")) {
      throw new NamingException("invalid ldaps URL: " + url + " expecting ldaps:///");
    }
    StringBuilder serviceName = new StringBuilder("_ldap._tcp");
    String domain = url.substring("ldaps:///".length(), url.length());
    String[] domainComponents = domain.split(",");
    for (String domainComponend : domainComponents) {
      if (!domainComponend.startsWith("dc=")) {
        throw new NamingException("invalid ldaps URL: " + url + " expecting dc=");
      }
      serviceName.append('.');
      serviceName.append(domainComponend, "dc=".length(), domainComponend.length());
    }
    return serviceName.toString();
  }

  private static void lookupService(String serviceName) throws NamingException {
    Hashtable<String, String> env = new Hashtable<>(4);
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
    env.put(Context.PROVIDER_URL, "dns:");
    DirContext context = new InitialDirContext(env);

    try {
      readFromContext(context, serviceName);
    } finally {
      context.close();
    }
  }

  static List<DnsRecord> readFromContext(DirContext context, String serviceName) throws NamingException {
    Attributes attribues = context.getAttributes(serviceName, SERVICE_NAMES);

    List<DnsRecord> records = new ArrayList<>(attribues.size());
    NamingEnumeration<?> services = attribues.get("srv").getAll();
    try {
      while (services.hasMore()) {
        String service = (String) services.next();
        DnsRecord record = DnsRecord.fromString(service);
        records.add(record);
      }
    } finally {
      services.close();
    }
    return records;
  }

  static LdapDnsProviderResult toDnsProviderResult(String domainName, List<DnsRecord> records) {
    return new LdapDnsProviderResult(domainName, convertToEndpoints(records));
  }

  private static List<String> convertToEndpoints(List<DnsRecord> records) {
    if (records.isEmpty()) {
      return List.of();
    }
    int minPriority = getMinPriority(records);
    return getRecordsWithPriorit(minPriority, records);
  }

  private static List<String> getRecordsWithPriorit(int priority, List<DnsRecord> records) {
    // REVIEW an argument can be made here that we should return either
    // - only one chosen by rand() considering the weight
    // - repetitions based on weight
    List<String> endpoints = new ArrayList<>(4);
    for (DnsRecord record : records) {
      if (record.getPriority() == priority) {
        endpoints.add(convertToLdapsUrl(record));
      }
    }
    return endpoints;
  }

  private static String convertToLdapsUrl(DnsRecord record) {
    String host = record.getHost();
    return "ldaps://" + host + ':' + LDAPS_PORT;
  }

  private static int getMinPriority(List<DnsRecord> records) {
    int minPriority = Integer.MAX_VALUE;
    for (DnsRecord record : records) {
      minPriority = Math.min(minPriority, record.getPriority());
    }
    return minPriority;
  }

  static final class DnsRecord {
    private final short priority;
    private final short weight;
    private final short port;
    private final String host;

    DnsRecord(int priority, int weight, int port, String host) {
      this.priority = (short) (priority & 0xFFFF);
      this.weight = (short) (weight & 0xFFFF);
      this.port = (short) (port & 0xFFFF);
      this.host = host;
    }

    int getPriority() {
      return Short.toUnsignedInt(this.priority);
    }

    int getWeight() {
      return Short.toUnsignedInt(this.weight);
    }

    int getPort() {
      return Short.toUnsignedInt(this.port);
    }

    String getHost() {
      // we need to extract the host name only for the hosts with the highest priority
      if (this.host.endsWith(".")) {
        return this.host.substring(0, this.host.length() - 1);
      }
      return this.host;
    }

    static DnsRecord fromString(String service) {
      String[] splitted = service.split(" ");
      String priority = splitted[0];
      String weight = splitted[1];
      String port = splitted[2];
      String host = splitted[3];
      return new DnsRecord(
              Integer.parseInt(priority),
              Integer.parseInt(weight),
              Integer.parseInt(port),
              host);
    }

  }

}
