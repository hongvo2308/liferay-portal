/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.journal.web.internal.portlet.action;

import com.liferay.data.engine.rest.resource.v2_0.DataDefinitionResource;
import com.liferay.journal.constants.JournalPortletKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;

import java.util.concurrent.Callable;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eudaldo Alonso
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + JournalPortletKeys.JOURNAL,
		"mvc.command.name=/journal/delete_data_definition"
	},
	service = MVCActionCommand.class
)
public class DeleteDataDefinitionMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long[] deleteDataDefinitionIds = null;

		long dataDefinitionId = ParamUtil.getLong(
			actionRequest, "dataDefinitionId");

		if (dataDefinitionId > 0) {
			deleteDataDefinitionIds = new long[] {dataDefinitionId};
		}
		else {
			deleteDataDefinitionIds = ParamUtil.getLongValues(
				actionRequest, "rowIds");
		}

		DataDefinitionResource.Builder dataDefinitionResourcedBuilder =
			_dataDefinitionResourceFactory.create();

		DataDefinitionResource dataDefinitionResource =
			dataDefinitionResourcedBuilder.user(
				themeDisplay.getUser()
			).build();

		for (long deleteDataDefinitionId : deleteDataDefinitionIds) {
			Callable<Long> callable = new DeleteDataDefinitionsCallable(
				dataDefinitionResource, deleteDataDefinitionId);

			try {
				TransactionInvokerUtil.invoke(_transactionConfig, callable);
			}
			catch (Throwable throwable) {
				SessionErrors.add(actionRequest, throwable.getClass());
			}
		}

		String redirect = ParamUtil.getString(actionRequest, "redirect");

		sendRedirect(actionRequest, actionResponse, redirect);
	}

	private long _deleteDataDefinition(
			DataDefinitionResource dataDefinitionResource,
			long deleteDataDefinitionId)
		throws Exception {

		dataDefinitionResource.deleteDataDefinition(deleteDataDefinitionId);

		return deleteDataDefinitionId;
	}

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED,
			new Class<?>[] {PortalException.class, SystemException.class});

	@Reference
	private DataDefinitionResource.Factory _dataDefinitionResourceFactory;

	private class DeleteDataDefinitionsCallable implements Callable<Long> {

		@Override
		public Long call() throws Exception {
			return _deleteDataDefinition(
				_dataDefinitionResource, _deleteDataDefinitionId);
		}

		private DeleteDataDefinitionsCallable(
			DataDefinitionResource dataDefinitionResource,
			long deleteDataDefinitionId) {

			_dataDefinitionResource = dataDefinitionResource;
			_deleteDataDefinitionId = deleteDataDefinitionId;
		}

		private final DataDefinitionResource _dataDefinitionResource;
		private final long _deleteDataDefinitionId;

	}

}