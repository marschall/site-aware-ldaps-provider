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

/**
 * Provides Site Aware LDAP server names, possibly using LDAPS.
 *
 * <p>The site can be specified by adding the {@link ActiveDirectoryContext#SITE}
 * {@link Context} environment attribute with the name of the site as a
 * {@link String}.</p>
 *
 * <p>This class is not meant to be instantiated directly by the user but rather
 * indirectly by the Java JNDI LDAP propvider.</p>
 *
 * @see ActiveDirectoryContext#SITE
 */
public final class SiteAwareLdapsProvider extends LdapDnsProvider {

  private static final int LDAP_PORT = 389;
  private static final int LDAPS_PORT = 636;
  private static final String[] SERVICE_NAMES = new String[] { "SRV" };

  // https://nitschinger.at/Bootstrapping-from-DNS-SRV-records-in-Java/
  // https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-dns.html
  // https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-ldap.html
  // https://bugs.openjdk.java.net/browse/JDK-8192975

  // _ldap._tcp.site-name._sites.dc._msdcs.example.com

  @Override
  public Optional<LdapDnsProviderResult> lookupEndpoints(String url, Map<?, ?> env) throws NamingException {
    // ldaps:///dc=example,dc=com
    DomainContextContext context = DomainContextContext.parse(url, (String) env.get(ActiveDirectoryContext.SITE));
    List<DnsRecord> records = lookupServiceRecords(context);
    LdapDnsProviderResult result = toDnsProviderResult(context, records);
    return Optional.of(result);
  }

  static final class DomainContextContext {

    private final String protocol;
    private final String baseDn;
    private final String[] domainComponents;
    private final String domainName;
    private final String site;

    private DomainContextContext(String protocol, String baseDn, String[] domainComponents, String site) {
      this.protocol = protocol;
      this.baseDn = baseDn;
      this.domainComponents = domainComponents;
      this.site = site;
      this.domainName = getDomainName(domainComponents);
    }

    private static String getDomainName(String[] domainComponents) {
      StringBuilder builderDomainName = new StringBuilder();
      for (int i = 0; i < domainComponents.length; i++) {
        if (i > 0) {
          builderDomainName.append('.');
        }
        String domainComponent = domainComponents[i];
        builderDomainName.append(domainComponent);
      }
      return builderDomainName.toString();
    }

    static DomainContextContext parse(String url, String site) throws NamingException {
      String protocol = getProtocol(url);
      String baseDn = getBaseDn(url);
      String[] domainComponents = getDomainComponents(baseDn);
      return new DomainContextContext(protocol, baseDn, domainComponents, site);
    }

    private static String getBaseDn(String url) {
      return url.substring(url.indexOf(":///") + ":///".length());
    }

    private static String[] getDomainComponents(String baseDn) throws NamingException {
      String[] parts = baseDn.split(",");
      String[] domainComponents = new String[parts.length];
      for (int i = 0; i < parts.length; i++) {
        String domainComponent = parts[i];
        if (!domainComponent.startsWith("dc=")) {
          throw new NamingException("invalid domain component: " + domainComponent + " expecting dc=");
        }
        domainComponents[i] = domainComponent.substring("dc=".length(), domainComponent.length());
      }
      return domainComponents;
    }

    private static String getProtocol(String url) throws NamingException {
      if (url.startsWith("ldaps:///")) {
        return "ldaps";
      }
      if (url.startsWith("ldap:///")) {
        return "ldap";
      }
      throw new NamingException("unknown protocol in URL " + url);
    }

    String getBaseDn() {
      return this.baseDn;
    }

    String getDomainName() {
      return this.domainName;
    }

    private int getPort() {
      if (this.protocol.equals("ldaps")) {
        return LDAPS_PORT;
      } else if (this.protocol.equals("ldap")) {
        return LDAP_PORT;
      }
      throw new IllegalStateException("unknown protocol: " + this.protocol);
    }

    String getServiceName() {
      // ldap:///dc=example,dc=com
      // _ldap._tcp.example.com
      StringBuilder serviceName = new StringBuilder("_ldap._tcp");
      if (this.site != null) {
        serviceName.append('.');
        serviceName.append(this.site);
        serviceName.append("._sites.dc._msdcs");
      }
      for (String domainComponent : this.domainComponents) {
        serviceName.append('.');
        serviceName.append(domainComponent);
      }
      return serviceName.toString();
    }

    String convertToLdapUrl(DnsRecord record) {
      String host = record.getHost();
      return this.protocol + "://" + host + ':' + this.getPort() + '/' + this.getBaseDn();
    }

  }

  private static List<DnsRecord> lookupServiceRecords(DomainContextContext domainContext) throws NamingException {
    Hashtable<String, String> env = new Hashtable<>(4);
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
    env.put(Context.PROVIDER_URL, "dns:");
    DirContext context = new InitialDirContext(env);

    try {
      return readFromContext(domainContext, context);
    } finally {
      context.close();
    }
  }

  static List<DnsRecord> readFromContext(DomainContextContext domainContext, DirContext context) throws NamingException {
    Attributes attribues = context.getAttributes(domainContext.getServiceName(), SERVICE_NAMES);

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

  static LdapDnsProviderResult toDnsProviderResult(DomainContextContext context, List<DnsRecord> records) {
    List<String> endpoints = convertToEndpoints(context, records);
    return new LdapDnsProviderResult(context.getDomainName(), endpoints);
  }

  private static List<String> convertToEndpoints(DomainContextContext context, List<DnsRecord> records) {
    if (records.isEmpty()) {
      return List.of();
    }
    int minPriority = getMinPriority(records);
    return getRecordsWithPriorit(context, minPriority, records);
  }

  private static List<String> getRecordsWithPriorit(DomainContextContext context, int priority, List<DnsRecord> records) {
    // REVIEW an argument can be made here that we should return either
    // - only one chosen by rand() considering the weight
    // - repetitions based on weight
    List<String> endpoints = new ArrayList<>(4);
    for (DnsRecord record : records) {
      if (record.getPriority() == priority) {
        endpoints.add(context.convertToLdapUrl(record));
      }
    }
    return endpoints;
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
      this.priority = toUnsignedShort(priority);
      this.weight = toUnsignedShort(weight);
      this.port = toUnsignedShort(port);
      this.host = host;
    }

    private static short toUnsignedShort(int i) {
      if (i < 0) {
        throw new IllegalArgumentException("negative number");
      }
      if (i > 0xFFFF) {
        throw new IllegalArgumentException("negative number");
      }
      return (short) (i & 0xFFFF);
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
      if (!this.host.isEmpty() && (this.host.charAt(this.host.length() - 1)  == '.')) {
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
