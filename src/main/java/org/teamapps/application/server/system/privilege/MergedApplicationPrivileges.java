package org.teamapps.application.server.system.privilege;

import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.privilege.PrivilegeGroup;
import org.teamapps.application.api.privilege.PrivilegeGroupType;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.ApplicationPrivilege;
import org.teamapps.model.controlcenter.ApplicationPrivilegeGroup;
import org.teamapps.ux.component.template.BaseTemplateTreeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MergedApplicationPrivileges extends BaseTemplateTreeNode<Object> {
	private final String name;
	private final Icon icon;
	private final String title;
	private final String description;
	private final Map<String, MergedPrivilegeGroup> privilegeGroupByName = new HashMap<>();

	public MergedApplicationPrivileges(String name, Icon icon, String title, String description) {
		this.name = name;
		this.icon = icon;
		this.title = title;
		this.description = description;
	}

	public void addPrivilegeGroup(PrivilegeGroup privilegeGroup, ApplicationLocalizationProvider localizationProvider) {
		MergedPrivilegeGroup mergedPrivilegeGroup = privilegeGroupByName.computeIfAbsent(privilegeGroup.getName(), s -> new MergedPrivilegeGroup(this, s, privilegeGroup.getType(), privilegeGroup.getIcon(), localizationProvider.getLocalized(privilegeGroup.getTitleKey()), localizationProvider.getLocalized(privilegeGroup.getDescriptionKey())));
		if (privilegeGroup.getPrivileges() != null) {
			for (Privilege privilege : privilegeGroup.getPrivileges()) {
				mergedPrivilegeGroup.getPrivilegesByName().computeIfAbsent(privilege.getName(), s -> new MergedPrivilege(mergedPrivilegeGroup, s, privilege.getIcon(), localizationProvider.getLocalized(privilege.getTitleKey())));
			}
		}
	}

	public void addPrivilegeGroup(ApplicationPrivilegeGroup privilegeGroup, ApplicationLocalizationProvider localizationProvider) {
		PrivilegeGroupType privilegeGroupType = PrivilegeGroupType.valueOf(privilegeGroup.getApplicationPrivilegeGroupType().name());
		MergedPrivilegeGroup mergedPrivilegeGroup = privilegeGroupByName.computeIfAbsent(privilegeGroup.getName(), s -> new MergedPrivilegeGroup(this, s, privilegeGroupType, IconUtils.decodeIcon(privilegeGroup.getIcon()), localizationProvider.getLocalized(privilegeGroup.getTitleKey()), localizationProvider.getLocalized(privilegeGroup.getDescriptionKey())));
		if (privilegeGroup.getPrivileges() != null) {
			for (ApplicationPrivilege privilege : privilegeGroup.getPrivileges()) {
				mergedPrivilegeGroup.getPrivilegesByName().computeIfAbsent(privilege.getName(), s -> new MergedPrivilege(mergedPrivilegeGroup, s, IconUtils.decodeIcon(privilege.getIcon()), localizationProvider.getLocalized(privilege.getTitleKey())));
			}
		}
	}

	public String getName() {
		return name;
	}

	public Icon getIcon() {
		return icon;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, MergedPrivilegeGroup> getPrivilegeGroupByName() {
		return privilegeGroupByName;
	}

	@Override
	public BaseTemplateTreeNode<Object> getParent() {
		return null;
	}

	public List<BaseTemplateTreeNode<Object>> getTreeRecords() {
		List<BaseTemplateTreeNode<Object>> nodes = new ArrayList<>();
		nodes.add(this);
		for (MergedPrivilegeGroup privilegeGroup : privilegeGroupByName.values()) {
			nodes.add(privilegeGroup);
			nodes.addAll(privilegeGroup.getPrivilegesByName().values());
		}
		return nodes;
	}

	@Override
	public String getBadge() {
		return privilegeGroupByName.values().stream().mapToInt(value -> Math.max(1, value.getPrivilegesByName().size())).sum() + "";
	}

	@Override
	public String getCaption() {
		return title;
	}
}
