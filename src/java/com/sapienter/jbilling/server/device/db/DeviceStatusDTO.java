package com.sapienter.jbilling.server.device.db;



import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.device.db.DeviceTypeDTO;
import com.sapienter.jbilling.server.device.db.DeviceDTO;
import com.sapienter.jbilling.server.device.db.UserDeviceDTO;
import com.sapienter.jbilling.server.device.Constants;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.AbstractGenericStatus;


@Entity
@DiscriminatorValue("device_status")
public class DeviceStatusDTO extends  AbstractGenericStatus implements java.io.Serializable {
	
	
	public DeviceStatusDTO() {
    }
    
    public DeviceStatusDTO(int statusValue) {
        this.statusValue = statusValue;
    }
   
	@Transient
	protected String getTable() {
		return Constants.TABLE_DEVICE_STATUS;
	}
	    
	   
	    

}
