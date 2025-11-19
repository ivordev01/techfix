package techfix.techfix.dashboard;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import techfix.techfix.ticket.TicketService;
import techfix.techfix.ticket.TicketService.TicketStatus;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private static final DateTimeFormatter NEXT_APPOINTMENT_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
	private final TicketService ticketService;

	public DashboardController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping
	public DashboardSummary dashboard() {
		var allTickets = ticketService.findAll();
		var pendingTickets = allTickets.stream().filter(t -> t.status() != TicketStatus.FINALIZADO).toList();

		return new DashboardSummary(pendingTickets.size(), 6,
				LocalDateTime.now().plusHours(2).format(NEXT_APPOINTMENT_FORMAT), allTickets.size(),
				pendingTickets.stream().map(this::convertTicket).toList());
	}

	private Ticket convertTicket(techfix.techfix.ticket.TicketService.Ticket ticket) {
		return new Ticket(ticket.id(), ticket.customerName(), ticket.device(), ticket.status().name(),
				ticket.entryDate());
	}

	public record DashboardSummary(int pendingTickets, int techniciansAvailable, String nextAppointment,
			int todayTickets, List<Ticket> tickets) {
	}

	public record Ticket(String id, String customer, String device, String status, String entryDate) {
	}
}

