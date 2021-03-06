package jbilling

import grails.plugins.springsecurity.Secured
import grails.util.Holders;

import com.sapienter.jbilling.common.SessionInternalError
import java.text.Format;
import java.text.SimpleDateFormat;

import javax.sql.DataSource;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean;
import in.saralam.sbs.server.RateCard.RateWS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import in.saralam.sbs.server.RateCard.db.RateDTO
import in.saralam.sbs.server.RateCard.db.RateDAS
import com.sapienter.jbilling.server.util.db.AbstractDAS
import com.sapienter.jbilling.server.item.db.ItemTypeDAS

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.client.ViewUtils
//import grails.plugins.springsecurity.Secured
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import org.hibernate.FetchMode
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Criterion
import org.hibernate.Criteria
import com.sapienter.jbilling.client.util.SortableCriteria
//import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import org.springframework.jdbc.datasource.DataSourceUtils
import javax.sql.DataSource

import au.com.bytecode.opencsv.CSVWriter
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import com.sapienter.jbilling.client.util.DownloadHelper
import org.apache.log4j.Logger;
import java.text.*;
import java.util.*;
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;

class RateController {

	static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]
	
		WebServicesSessionSpringBean webServicesSession = new  WebServicesSessionSpringBean();
		def filterService
	
		ViewUtils viewUtils
		DataSource dataSource
		def breadcrumbService
		
		
		def index = {
        redirect (action: 'list', params: params)
    }

    def private getFilteredRates(filters, GrailsParameterMap params) {
	
		
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return RateDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
           
		    
            and {
                filters.each { filter ->
                       log.debug("fileter  field ${filter.field}")
                    if (filter.value) {                                   
                            addToCriteria(filter.getRestrictions());
                       }
                    }					  
                }   
				 eq('entity', new CompanyDTO(session['company_id']))		
                 eq('deleted', 0)            
		
            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }

    def list = {
		log.debug("first comes in action list when click on Bundle");
        def filters = filterService.getFilters(FilterType.RATE, params)
        log.debug(" filters" + filters)
        def rates = getFilteredRates(filters, params)
         log.debug(" rates" + rates)
		 bindData(rates, params)
		 log.debug " rate params is ${params}"
	
	   CurrencyDTO currency = new CurrencyDAS().find( new CurrencyBL().getEntityCurrency( session['company_id'].toInteger()));
	  
        def selected = params.id ? webServicesSession.getRateWS(params.int("id")) : null
       
        log.debug(" selected "+selected)
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)

        if (params.applyFilter || params.partial) {
            render template: 'rates', model: [ rates: rates, selected: selected,currency:currency, filters: filters ]
        } else {
            [ rates: rates, selected: selected,currency:currency, filters: filters ]
        }
    }
		
	
			/* @Secured(["ORDER_24"]) */
	
		def show = {
			RateWS ratews = webServicesSession.getRateWS(params.int('id'))
			CurrencyDTO currency = new CurrencyDAS().find( new CurrencyBL().getEntityCurrency( session['company_id'].toInteger()));
			render template:'show', model: [ratews: ratews,currency:currency]
		}
	
	
	
		def edit = {
			params.max = params?.max?.toInteger() ?: pagination.max
			params.offset = params?.offset?.toInteger() ?: pagination.offset
			
			Integer entityId = webServicesSession.getCallerCompanyId();
			def description = "Plans";
			Integer catId = new ItemTypeDAS().findByDescription(entityId, description).getId();
		
			log.debug(" Fetching Plan type products for entity  " + entityId + " description = " + description + " Item Type Id " + catId);
			def planList = webServicesSession.getItemByCategory(catId);
		
			if(planList == null ) {
				log.warning("No Rating products Found... create a product in Plans category");
			} else {
				log.debug(planList.length + " Rating product found ");
			}
		
			
			render template: 'edit', model: [planList: planList]
			
		}
	
	
	
	def save = {
	
		log.debug("In Rate Controllers save action");
		def rate = new RateWS();
		//bindData(rate, params)
	    def errorCount=0;
		def temp = null
		
		try {
			// save or update
			def ratefile = request.getFile("ratefile")
			log.debug("rate input file " + ratefile + "content " + params.ratefile?.getContentType().toString());
				  
			if(!ratefile.empty) {
				def grailsApplication = Holders.getGrailsApplication()	
				
				//def tempDir = grailsApplication.config.dir.variable as String
				//tempDir = tempDir + "//resources//temp"
				//log.debug " directory to store the files ${tempDir}"
				

				/*File dir = new File(tempDir)
				
				if(!dir.exists()){					
					dir.mkdir()
				}*/
								
				def csvFile = File.createTempFile("ratecard",".csv")				
				ratefile.transferTo(csvFile)				
				File file = new File(csvFile?.getAbsolutePath());				
				BufferedReader reader = new BufferedReader(new java.io.FileReader(file));
				csvFile.deleteOnExit()
	
				log.debug("rate csv saved to: " + csvFile?.getAbsolutePath());
									
				String[] cols = new Object[7]
				def idx
				def ratews
				def y
				String line
				def i
	
				while((line = reader.readLine())!= null && line.length()!= 0) {
					if(line.startsWith("#")) continue;	//comment
					if(("").equals(line)) continue;		//blank line
	
					cols = line.split(",", -1);
					if(cols.size() !=8) {
						flash.error = 'file is not in format'
						continue;
					}
							
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
					
					def col0 = cols[0];
					def col1 = cols[1];
		
					def today = new Date()
					Date lastUpdatedDate = today;
					//idx = col0.indexOf("F089");
					String prefix = col0;
					String destination = col1;
					String rateType=cols[7];
                    BigDecimal flatRate;
                    BigDecimal conncharge;
                    BigDecimal scaledRate;
                                          
                    if(!cols[2].isEmpty()){
                        flatRate = new BigDecimal(cols[2]); }
                    if(!cols[3].isEmpty()){
						conncharge = new BigDecimal(cols[3]); }
                    if(!cols[4].isEmpty()){
						scaledRate = new BigDecimal(cols[4]); }
					
                    Date createdDate = today;
					Date validFrom = today;
					Date validTo = null;
					
							
					sdf.setLenient(false);
					Calendar c = Calendar.getInstance();
							
					if (cols[5] != "") {
						validFrom = sdf.parse(cols[5]);
						c.setTime(validFrom);
						int vf = c.get(Calendar.YEAR);
						log.debug("valid from is : " +validFrom);
						if (vf > 1000){
							log.debug("valid from year..." +vf);
						}
						else{
							continue;
						}
					}
					if(cols[6] != ""){
						validTo = sdf.parse(cols[6]);
						c.setTime(validTo);
						int vt = c.get(Calendar.YEAR);
						log.debug("valid to is : " +validTo);
						if (vt > 1000){
							log.debug("valid to  year..." +vt);
						}else{
							continue;
						}
					} 
						
					Integer plan =   params.int('toItemId')
                    log.debug(" selected rate  plan " + plan)
					
					if(plan == null) {
						flash.error= 'Missing Plan or Rates File ! Please select Plan and Rates File'
						/*flash.message= 'Missing Plan or Rates File ! Please select Plan and Rates File'*/
						          
					}
								
                    if(plan != null){
                        errorCount++
                        def entityId = new CompanyDTO(session['company_id'])						
						log.debug("ratews method is called; with entity id " +entityId);
						ratews = new RateWS(prefix, destination, flatRate, conncharge, scaledRate, plan, createdDate, validFrom, validTo, lastUpdatedDate,rateType,entityId);
						log.debug("ratews is with : " + prefix + destination + flatRate + conncharge + scaledRate);
						y = webServicesSession.createRate(ratews);
                    }						  
				}				
			}
				
					
			//def ratewsd = new RateWS();
			if(errorCount!=0) {
				flash.message = 'RATE CARD deatils are Sucessfully Loaded'
			}else{
				flash.info = 'Failed load Rates from file'
			}										   
		}catch (SessionInternalError e) {
				viewUtils.resolveException(flash, session.locale, e)
				chain action: 'list', model: [ selected: rate ]
				return
	
		}finally {
			temp?.delete()
		}				
	
		chain action: 'list', params: [ id: rate?.id ]		
	}	
	
	
	def splitfields(line) {
	
		log.debug("splitfields is called");
		return line.split(",", -1);
	}
	
    def deleteRate = {
		log.debug "delete action is triggered"
        if (params.id) {
            webServicesSession.deleteRate(params.int('id'))

            flash.message = 'rate.deleted'
            flash.args = [ params.id ]
            log.debug("Deleted rate ${params.id}.")

        }

        // render the partial user list
        params.partial = true
        list()
    }
	  
	def getCurrencies() {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return currencies.findAll{ it.inUse }
    }

	
	def csv = {
		def filters = filterService.getFilters(FilterType.RATE, params)
	
		params.max = CsvExporter.MAX_RESULTS
		def rates = getFilteredRates(filters, params)
	
		if (rates.totalCount > CsvExporter.MAX_RESULTS) {
			flash.error = message(code: 'error.export.exceeds.maximum')
			flash.args = [ CsvExporter.MAX_RESULTS ]
			redirect action: 'list', id: params.id
	
		} else {
			DownloadHelper.setResponseHeader(response, "rates.csv")
			Exporter<RateDTO> exporter = CsvExporter.createExporter(RateDTO.class);
			render text: exporter.export(rates), contentType: "text/csv"
		}
	}
		
  Integer entityId = webServicesSession.getCallerCompanyId();
       	  def description = "Plans";
	      def planList = webServicesSession.getItemByCategory(new ItemTypeDAS().findByDescription(entityId, description).getId());

}
