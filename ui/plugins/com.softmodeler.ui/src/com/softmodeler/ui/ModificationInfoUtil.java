/*******************************************************************************
 * $URL: svn://176.28.48.244/projects/softmodeler/trunk/ui/plugins/com.softmodeler.ui/src/com/softmodeler/ui/ModificationInfoUtil.java $
 *
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui;

import java.util.Date;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;

import com.softmodeler.common.CommonPlugin;
import com.softmodeler.common.ServerException;
import com.softmodeler.model.ModificationInfo;
import com.softmodeler.model.ObjectRef;
import com.softmodeler.security.ISession;
import com.softmodeler.service.IModificationInfoService;

/**
 * @author created by Author: fdo, last update by $Author: fdo $
 * @version $Revision: 17842 $, $Date: 2012-12-21 14:25:51 +0100 (Fri, 21 Dec 2012) $
 */
public class ModificationInfoUtil {
	/** access to the modification information service */
	private static IModificationInfoService modificationInfoService = null;

	/**
	 * returns the modification information service
	 * 
	 * @return
	 */
	private static IModificationInfoService getModificationInfoService() {
		if (modificationInfoService == null) {
			modificationInfoService = CommonPlugin.getService(IModificationInfoService.class);
		}
		return modificationInfoService;
	}

	/**
	 * returns the {@link ModificationInfo} for the current user
	 * 
	 * @return
	 * @throws ServerException
	 */
	public static WritableList getModificationInfo() throws ServerException {
		ISession session = CommonPlugin.getUserService().getSession();
		String sessionKey = "modificationInfoList"; //$NON-NLS-1$
		WritableList modificationInfoList = (WritableList) session.getValue(sessionKey);
		if (modificationInfoList == null) {
			String individualId = CommonPlugin.getUserService().getIndividualId();
			int domain = CommonPlugin.getUserService().getDomain();
			List<ModificationInfo> list = getModificationInfoService().findActiveByUser(individualId, domain);
			modificationInfoList = new WritableList(list, ModificationInfo.class);
			session.setValue(sessionKey, modificationInfoList);
		}
		return modificationInfoList;
	}

	/**
	 * finds the {@link ModificationInfo} according to the passed objectRef and adds it to the ModificationInfo cache<br/>
	 * only if it is modified
	 * 
	 * @param objectRef
	 * @throws ServerException
	 */
	public static void addModificationInfo(ObjectRef objectRef) throws ServerException {
		ModificationInfo modificationInfo = getModificationInfoService().findByObjectId(objectRef.getId());
		if (modificationInfo != null && modificationInfo.isModified()) {
			WritableList objects = getModificationInfo();
			for (Object object : objects) {
				ModificationInfo m = (ModificationInfo) object;
				if (modificationInfo.getObjectRef().equals(m.getObjectRef())) {
					objects.remove(object);
					break;
				}
			}
			objects.add(0, modificationInfo);
		}
	}

	/**
	 * is there a {@link ModificationInfo} that reveres to the passed objectRef
	 * 
	 * @param objectRef
	 * @return
	 */
	public static boolean hasModificationInfo(ObjectRef objectRef) {
		try {
			for (Object object : getModificationInfo()) {
				ModificationInfo modificationInfo = (ModificationInfo) object;
				if (modificationInfo.getObjectRef().equals(objectRef)) {
					return true;
				}
			}
		} catch (Exception e) {
			UIPlugin.logger.warn("error with modification info", e); //$NON-NLS-1$
		}
		return false;
	}

	/**
	 * remove a {@link ModificationInfo} according to the objectRef
	 * 
	 * @param objectRef
	 * @return
	 */
	public static boolean removeModificationInfo(ObjectRef objectRef) {
		try {
			for (Object object : getModificationInfo()) {
				ModificationInfo modificationInfo = (ModificationInfo) object;
				if (modificationInfo.getObjectRef().equals(objectRef)) {
					getModificationInfo().remove(modificationInfo);
					return true;
				}
			}
		} catch (Exception e) {
			UIPlugin.logger.warn("error with modification info", e); //$NON-NLS-1$
		}
		return false;
	}

	/**
	 * lock the object and update the local modification info state
	 * 
	 * @param objectRef
	 */
	public static void lock(ObjectRef objectRef) {
		try {
			if (objectRef != null) {
				getModificationInfoService().lockById(objectRef.getId());

				WritableList modificationInfos = getModificationInfo();
				for (Object object : modificationInfos) {
					ModificationInfo modificationInfo = (ModificationInfo) object;
					if (modificationInfo.getObjectRef().equals(objectRef)) {
						// if modification info exists, update lock data
						modificationInfo.setLockDate(new Date());
						modificationInfo.setLocker(CommonPlugin.getUserService().getUser().getName());
						modificationInfos.set(modificationInfos.indexOf(modificationInfo), modificationInfo);
						return;
					}
				}

				// if there was no existing modification info, read from server
				ModificationInfo modificationInfo = getModificationInfoService().findByObjectId(objectRef.getId());
				modificationInfos.add(0, modificationInfo);
			}
		} catch (Exception e) {
			UIUtil.handleException(e);
		}
	}

	/**
	 * unlock the object and update the local modification info state
	 * 
	 * @param objectRef
	 */
	public static void unlock(ObjectRef objectRef) {
		try {
			if (objectRef != null) {
				getModificationInfoService().unlockById(objectRef.getId());

				WritableList modificationInfos = getModificationInfo();
				for (Object object : modificationInfos) {
					ModificationInfo modificationInfo = (ModificationInfo) object;
					if (modificationInfo.getObjectRef().equals(objectRef)) {
						if (modificationInfo.isModified()) {
							modificationInfo.setLocker(null);
							modificationInfo.setLockDate(null);
							modificationInfos.set(modificationInfos.indexOf(modificationInfo), modificationInfo);
						} else {
							// remove not if modified
							modificationInfos.remove(modificationInfo);
						}
						return;
					}
				}
			}
		} catch (Exception e) {
			UIUtil.handleException(e);
		}
	}
}
