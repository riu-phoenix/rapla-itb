
/*--------------------------------------------------------------------------*
 | Copyright (C) 2006  Christopher Kohlhaas                                 |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/

package org.rapla.plugin.timeslot;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.GroupStartTimesStrategy;
import org.rapla.components.calendarview.swing.AbstractSwingCalendar;
import org.rapla.components.calendarview.swing.SwingBlock;
import org.rapla.components.calendarview.swing.SwingCompactWeekView;
import org.rapla.components.calendarview.swing.ViewListener;
import org.rapla.components.util.DateTools;
import org.rapla.components.util.TimeInterval;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.plugin.abstractcalendar.AbstractRaplaBlock;
import org.rapla.plugin.abstractcalendar.AbstractRaplaSwingCalendar;
import org.rapla.plugin.abstractcalendar.RaplaBuilder;
import org.rapla.plugin.abstractcalendar.RaplaCalendarViewListener;
import org.rapla.plugin.abstractcalendar.SwingRaplaBlock;
import org.rapla.plugin.abstractcalendar.SwingRaplaBuilder;


public class SwingCompactDayCalendar extends AbstractRaplaSwingCalendar
{
	List<Timeslot> timeslots;
	
    public SwingCompactDayCalendar(RaplaContext sm,CalendarModel settings, boolean editable) throws RaplaException {
        super( sm, settings, editable);
    }
    
    protected AbstractSwingCalendar createView(boolean showScrollPane) throws RaplaException 
    {
    	SwingCompactWeekView compactWeekView = new SwingCompactWeekView( showScrollPane ) {
            @Override
            protected JComponent createColumnHeader(Integer column) {
                JLabel component = (JLabel) super.createColumnHeader(column);
                if ( column != null)
                {
	                try {
	                	List<Allocatable> sortedAllocatables = getSortedAllocatables();
	          		  	Allocatable allocatable = sortedAllocatables.get(column);
	          		  	String name = allocatable.getName( getLocale());
	          		  	component.setText( name);
	                } catch (RaplaException e) {
					}
                }
                return component;
            }
            protected int getColumnCount() 
        	{
            	try {
          		  Allocatable[] selectedAllocatables =model.getSelectedAllocatables();
          		  return selectedAllocatables.length;
            	  	} catch (RaplaException e) {
            	  		return 0;
            	  	}
        	}
            
            @Override
            public TimeInterval normalizeBlockIntervall(SwingBlock block) 
            {
            	Date start = block.getStart();
				Date end = block.getEnd();
				for (Timeslot slot:timeslots)
				{
					int minuteOfDay = DateTools.getMinuteOfDay( start.getTime());
					if ( minuteOfDay >= slot.minuteOfDay)
					{
						start = new Date(DateTools.cutDate( start).getTime() + slot.minuteOfDay);
						break;
					}
				}
				for (Timeslot slot:timeslots)
				{
					int minuteOfDay = DateTools.getMinuteOfDay( end.getTime());
					if ( minuteOfDay < slot.minuteOfDay)
					{
						end = new Date(DateTools.cutDate( end).getTime() + slot.minuteOfDay);
					}
					if (  slot.minuteOfDay > minuteOfDay)
					{
						break;
					}
						
				}
				return new TimeInterval(start,end);
            }

        };
        compactWeekView.setDaysInView(1);
		return compactWeekView;

    }

    protected ViewListener createListener() throws RaplaException {
        return  new RaplaCalendarViewListener(getContext(), model, view.getComponent()) {
        	@Override
        	protected Collection<Allocatable> getMarkedAllocatables() 
        	{
        		List<Allocatable> selectedAllocatables = getSortedAllocatables();
                int columns = selectedAllocatables.size();
                Set<Allocatable> allSelected = new HashSet<Allocatable>();
                int slots = columns*timeslots.size();
				for ( int i=0;i<slots;i++) 
                {
                	if ( ((SwingCompactWeekView)view).isSelected(i))
                	{
                		int column = i%columns;
                		Allocatable allocatable = selectedAllocatables.get( column);
						allSelected.add( allocatable);
                	}
                }
            	if ( selectedAllocatables.size() == 1 ) {
					allSelected.add(selectedAllocatables.get(0));
				}
            	return allSelected;
        	
        	}
        	/** override to change the allocatable to the row that is selected */
        	@Override
            protected void showPopupMenu(Component component,Point p,Date start,Date end, int slotNr)
            {
        		TimeInterval intervall = getMarkedInterval(start);
            	Collection<Allocatable> allSelected = getMarkedAllocatables();
				showContextPopup( component, p, intervall.getStart(),intervall.getEnd(),  allSelected);
            }
			public void selectionChanged(Date start,Date end) 
            {
            	TimeInterval inter = getMarkedInterval(start);
        		super.selectionChanged(inter.getStart(), inter.getEnd());
            }
			
			protected TimeInterval getMarkedInterval(Date start) {
				List<Allocatable> selectedAllocatables = getSortedAllocatables();
				int columns = selectedAllocatables.size();
				Date end;
				Integer startTime = null;
		        Integer endTime = null;
		        int slots = columns*timeslots.size();
				
		        for ( int i=0;i<slots;i++) 
		        {
		        	if ( ((SwingCompactWeekView)view).isSelected(i))
		        	{
		        		int index = i/columns;
		        		int time = timeslots.get(index).minuteOfDay;
						if ( startTime == null || time < startTime )
		        		{
		        			startTime = time;
		        		}
						
						time = index<timeslots.size()-1 ? timeslots.get(index+1).minuteOfDay : 24* 60;
						if ( endTime == null || time >= endTime )
		        		{
		        			endTime = time;
		        		}
		        	}
		        }
		        
		        if ( startTime == null)
		        {
		        	startTime = getCalendarOptions().getWorktimeStart() * 60;
		        }
		        if ( endTime == null)
		        {
		        	endTime = getCalendarOptions().getWorktimeEnd() * 60;
		        }
		        
		        Calendar cal = getRaplaLocale().createCalendar();
		        cal.setTime ( start );
		        cal.set( Calendar.HOUR_OF_DAY, startTime/60);
		        cal.set( Calendar.MINUTE, startTime%60);
		        
		        start = cal.getTime();
		        cal.set( Calendar.HOUR_OF_DAY, endTime/60);
		        cal.set( Calendar.MINUTE, endTime%60);
			      
		        end = cal.getTime();
		        TimeInterval intervall = new TimeInterval(start,end);
				return intervall;
			}

        	
        	 @Override
			 public void moved(Block block, Point p, Date newStart, int slotNr) {
				 int index= slotNr;//getIndex( selectedAllocatables, block );
				
				 if ( index < 0)
				 {
					 return;
				 }
				    
				 try 
				 {
					 final List<Allocatable> selectedAllocatables = getSortedAllocatables();
					 int columns = selectedAllocatables.size();
					 int column = index%columns;
					 Allocatable newAlloc = selectedAllocatables.get(column);
					 AbstractRaplaBlock raplaBlock = (AbstractRaplaBlock)block;
					 Allocatable oldAlloc = raplaBlock.getGroupAllocatable();
					 Appointment app = raplaBlock.getAppointment();
					 if ( newAlloc != null && oldAlloc != null && !newAlloc.equals(oldAlloc))
					 {
						 Reservation reservation = raplaBlock.getReservation();
						 getReservationController().exchangeAllocatable(reservation,app,newStart, oldAlloc,newAlloc, getMainComponent(),p);
					 }
					 else
					 {
						 int rowIndex = index/columns;
						 Timeslot timeslot = timeslots.get(rowIndex);
						 int time = timeslot.minuteOfDay;
							
						 Calendar cal = getRaplaLocale().createCalendar();
						 cal.setTime ( newStart );
						 cal.set( Calendar.HOUR_OF_DAY, time /60);
						 cal.set( Calendar.MINUTE, time %60);
					        
						 newStart = cal.getTime();
			             SwingRaplaBlock b = (SwingRaplaBlock) block;
			             try {
			            	 boolean keepTime = false;
			                 getReservationController().moveAppointment(b.getAppointment()
			                                                            ,b.getStart()
			                                                            ,newStart
			                                                            ,calendarContainerComponent
			                                                            ,p, keepTime);
			             } catch (RaplaException ex) {
			                 showException(ex,b.getView());
			             }
					 }
				 } 
				 catch (RaplaException ex) {
					showException(ex, getMainComponent());
				}
			
			 }


        };
    }
    
	

    
    protected RaplaBuilder createBuilder() throws RaplaException 
    {
    	timeslots = getService(TimeslotProvider.class).getTimeslots();
    	List<Integer> startTimes = new ArrayList<Integer>();
    	for (Timeslot slot:timeslots) {
    		 startTimes.add( slot.getMinuteOfDay());
    	}
    	RaplaBuilder builder = new SwingRaplaBuilder(getContext());
        builder.setRepeatingVisible( view.isEditable());
        builder.setEditingUser( getUser() );
        builder.setExceptionsExcluded( !getCalendarOptions().isExceptionsVisible() || !view.isEditable());
        Date startDate = view.getStartDate();
		Date endDate = view.getEndDate();
        final List<Allocatable> allocatables = getSortedAllocatables();
        builder.selectAllocatables( allocatables);
		builder.setFromModel( model, startDate, endDate );

        builder.setSmallBlocks( true );
        GroupStartTimesStrategy strategy = new GroupStartTimesStrategy();
        strategy.setAllocatables(allocatables);
        strategy.setFixedSlotsEnabled( true);
        strategy.setResolveConflictsEnabled( false );
        strategy.setStartTimes( startTimes );
        builder.setBuildStrategy( strategy);

        String[] slotNames = new String[ timeslots.size() ];
        int maxSlotLength = 5;
        for (int i = 0; i <timeslots.size(); i++ ) {
        	String slotName = timeslots.get( i ).getName();
        	maxSlotLength = Math.max( maxSlotLength, slotName.length());
			slotNames[i] = slotName;
        }
        ((SwingCompactWeekView)view).setLeftColumnSize( 30+ maxSlotLength * 6);
        builder.setSplitByAllocatables( false );
    
        ((SwingCompactWeekView)view).setSlots( slotNames );
        return builder;
    }

    protected void configureView() throws RaplaException {
        view.setToDate(model.getSelectedDate());
//        if ( !view.isEditable() ) {
//            view.setSlotSize( model.getSize());
//        } else {
//            view.setSlotSize( 200 );
//        }
    }

    public int getIncrementSize()
    {
    	return Calendar.DATE;
    }


}
