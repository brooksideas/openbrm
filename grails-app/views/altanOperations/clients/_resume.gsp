<div class="column-hold">
	<div class="heading">
		<strong><g:message code="Reanudación" /></strong>
	</div>
	<div class="box">
		<g:form name="operation_form" controller="altanOperations"
			action="operationResult">
			<label>MSISDN: </label>
			<g:textField name="msisdn" />
			<br />
		</g:form>
	</div>
	<div class="btn-box">
		<g:remoteLink class="cell double" action="operationResult" id="4_7"
			params="{msisdn:\$('#msisdn').val()}"
			before="register(this);" onSuccess="render(data, next);">
			<strong>Reanudar</strong>
		</g:remoteLink>
	</div>
</div>