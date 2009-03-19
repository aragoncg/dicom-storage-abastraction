function validateChecks(check,check_type,total)  //checks if at least <<total>> checks have been checked
{
	var checked_num=0;
	for (var i = 0; i < check.length; i++)
	{
		if (check[i].checked)
			checked_num++;
	}
	
	if (checked_num >= total)
		return true;
	
	var word = "checkbox";
	if (total>1)
		word = "checkboxes";
		
	alert('Please check at least ' + total + ' (' + check_type + ') ' + word);
	return false;
}

function validateChecksAndConfirm(check,check_type,total,confirmMsg)  //checks if at least <<total>> checks have been checked
{
	if ( validateChecks(check,check_type, total) ) {
		return confirm(confirmMsg);
	}
	return false;
}
function validateRadios(radios, radio_type)  //checks if a radio button have been selected
{
	for (var i = 0; i < radios.length; i++)
	{
		if (radios[i].checked)
			return true;
	}
	alert('Please select a ' + radio_type);
	return false;
}

function checkNotEmpty( field,field_name ) //checks if a field value is not null and not empty
{
	if ( isBlank(field.value) ) {
		alert('Field ' + field_name + ' is empty!' );
		field.focus();
		return false;
	}
	return true;
}

function checkPatientFields( form ) //checks if certain patient fields are not empty
{
	if ( isBlank(form.patientID.value) ) {
		alert('Field Patient ID is empty!' );
		form.patientID.focus();
		return false;
	}
	if ( isBlank(form.issuerOfPatientID.value) ) {
		alert('Field Issuer of Patient ID is empty!' );
		form.issuerOfPatientID.focus();
		return false;
	}
	if ( isBlank(form.patientName.value) ) {
		alert('Field Patient Name is empty!' );
		form.patientName.focus();
		return false;
	}
	return true;
}

//-------------------------------------------------------------------
// isBlank(value)
//   Returns true if value only contains spaces
//-------------------------------------------------------------------
function isBlank(val){
	if(val==null){return true;}
	for(var i=0;i<val.length;i++) {
		if ((val.charAt(i)!=' ')&&(val.charAt(i)!="\t")&&(val.charAt(i)!="\n")&&(val.charAt(i)!="\r")){
			return false;
		}
	}
	return true;
}

function checkPopup( popupMsg )
{
	if ( popupMsg != '' ) {
		alert( popupMsg );
	}
}

function selectCipher()
{
	selection = document.ae_edit.cipherSelect.options[document.ae_edit.cipherSelect.selectedIndex ].value;
	if ( selection != '--' ) {
		document.ae_edit.cipherSuites.value=document.ae_edit.cipherSelect.options[document.ae_edit.cipherSelect.selectedIndex ].value;
	}
}

function doEchoAET(aet)  //open echo window
{
	newwindow = window.open('aeecho.m?aet='+aet, 'ECHO', 'toolbar=no,menubar=no, scrollbars=no, width=500,height=200');
	newwindow.focus();
	return false;
}

function doEcho(form)  //open echo window
{
	var query = 'title='+form.title.value+'&hostName='+form.hostName.value+'&port='+form.port.value;
	query = query + '&cipher1='+selectValue(form.cipher1)+'&cipher2='+selectValue(form.cipher2)+'&cipher3='+selectValue(form.cipher3)
	newwindow = window.open('aeecho.m?'+query, 'ECHO', 'toolbar=no,menubar=no, scrollbars=no, width=500,height=200');
	newwindow.focus();
	return false;
}

function selectValue(sObj) {
    with (sObj) return options[selectedIndex].value;
  }
  
function openWin( winName, url ) {
//alert('open new window '+winName+' with URL:'+url);
	newwindow = window.open(url, winName, 'toolbar=no,menubar=no,scrollbars=yes,resizable=yes');
	newwindow.focus();
	return false;
}  

/*
* Select/Deselect all checkboxes of given Form.
* form... HTML Form.
* name... Name of checkbox element to select 
*         (all checkboxes which name starts with this value will match)
*         e.g. sticky -> all stickyPat, stickyStudy, stickySeries and stickyInst will selected/deselected.
* chk... true select; false deselect.
*/
function selectAll(form, name, chk) {
	var elems = form.elements;
	var pos;
	if ( name != null )
		pos = name.length;
	for ( var i = 0 ; i < elems.length ; i++ ) {
		var e = elems[i];
		if ( ((name == null) || (e.name.substring(0,pos) == name)) && (e.type == 'checkbox')) {
			e.checked = chk;
    	}
	}
}

function toggle(check1, check2) {
    if (check1.checked) {
        check2.checked = false;
    }
}
  
  