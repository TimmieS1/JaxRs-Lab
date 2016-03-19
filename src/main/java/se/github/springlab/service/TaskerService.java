package se.github.springlab.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.github.springlab.exception.InvalidItemException;
import se.github.springlab.exception.InvalidTeamException;
import se.github.springlab.exception.InvalidUserException;
import se.github.springlab.model.Issue;
import se.github.springlab.model.Team;
import se.github.springlab.model.User;
import se.github.springlab.model.WorkItem;
import se.github.springlab.repository.IssueRepository;
import se.github.springlab.repository.TeamRepository;
import se.github.springlab.repository.UserRepository;
import se.github.springlab.repository.WorkItemRepository;
import se.github.springlab.status.ItemStatus;

@Service
public class TaskerService
{
	private UserRepository userRepository;
	private TeamRepository teamRepository;
	private WorkItemRepository workItemRepository;
	private IssueRepository issueRepository;

	@Autowired
	public TaskerService(UserRepository userRepository, TeamRepository teamRepository, WorkItemRepository workItemRepository, IssueRepository issueRepository)
	{
		this.userRepository = userRepository;
		this.teamRepository = teamRepository;
		this.workItemRepository = workItemRepository;
		this.issueRepository = issueRepository;
	}

	// ------------------TEAM------------------

	public void update(Team team)
	{
		teamRepository.save(team);
		if (team.isActive() == false)
		{
			for (User user : userRepository.findByTeam(team))
			{
				user.assignTeam(null);
				update(user);
			}
		}
	}

	// ------------------USER------------------

	public void update(User user)
	{
		if (user.getUsername().length() > 10)
		{
			throw new InvalidUserException("Username cannot exceed 10 characters!");
		}
		if (user.isActive() == false)
		{
			List<WorkItem> workItems = workItemRepository.findByAssignedUser(user);
			for (WorkItem workItem : workItems)
			{
				workItem.setStatus(ItemStatus.UNSTARTED);
				update(workItem);
			}
		}
		if (!user.hasId())
		{
			if (userRepository.findByTeam(user.getTeam()).size() > 9)
			{
				throw new InvalidTeamException("team does not allow more than 10 users at any time");
			}
		}
		userRepository.save(user);
	}

	//	public List<User> getAll()
	//	{
	//		return userRepository.findAll();
	//	}

	// -----------------WORKITEM-------------------

	public void update(WorkItem workItem)
	{
		if (workItem.getAssignedUser() == null)
		{
			throw new InvalidUserException("Cannot persist item with no user assigned!");
		}
		if (workItem.getAssignedUser().isActive() == false)
		{
			throw new InvalidUserException("Cannot assign item to inactive user!");
		}
		if (!workItem.hasId())
		{
			if (workItemRepository.findByAssignedUser(workItem.getAssignedUser()).size() > 4)
			{
				throw new InvalidUserException("cannot store more than 5 workitems at any time");
			}
		}
		workItemRepository.save(workItem);
	}

	public List<WorkItem> getByStatus(ItemStatus status)
	{
		return workItemRepository.findByItemStatus(status.ordinal());
	}

	@Transactional
	public void removeItem(WorkItem workItem)
	{
		workItem.assignUser(null);
		for (Issue issue : issueRepository.findByWorkItem(workItem))
		{
			issue.setWorkItem(null);
		}
		workItemRepository.delete(workItem.getId());
	}

	// -------------------- ISSUE -------------------- //
	public void update(Issue issue)
	{
		if (issue.getWorkItem().getStatus() == ItemStatus.DONE)
		{
			issue.getWorkItem().setStatus(ItemStatus.UNSTARTED);
			issueRepository.save(issue);
		}
		else
		{
			throw new InvalidItemException("Cannot add issue to unfinished work item!");
		}
	}

	public Set<WorkItem> getItemsWithIssue()
	{
		Set<WorkItem> wItems = new HashSet<>();
		for (Issue issue : issueRepository.findAll())
		{
			wItems.add(issue.getWorkItem());
		}
		return wItems;
	}

}