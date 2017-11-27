package dk.kb.netarchivesuite.solrwayback.service.dto.statistics;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DomainYearStatistics {

  private int year;
  private int links;
  private int sizeInKb;
  private int totalPages;
  private String domain;
  
  public  DomainYearStatistics(){       
  }


  public int getYear() {
    return year;
  }


  public void setYear(int year) {
    this.year = year;
  }


  public int getLinks() {
    return links;
  }


  public void setLinks(int links) {
    this.links = links;
  }


  public int getSizeInKb() {
    return sizeInKb;
  }


  public void setSizeInKb(int sizeInKb) {
    this.sizeInKb = sizeInKb;
  }


  public int getTotalPages() {
    return totalPages;
  }


  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }


  public String getDomain() {
    return domain;
  }


  public void setDomain(String domain) {
    this.domain = domain;
  }
  
  
  
  
}
