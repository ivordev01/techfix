package techfix.techfix.ticket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import techfix.techfix.customer.CustomerService;
import techfix.techfix.customer.CustomerService.Customer;

@Service
public class TicketService {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private final AtomicInteger counter = new AtomicInteger(1048);
	private final ConcurrentMap<String, Ticket> tickets = new ConcurrentHashMap<>();
	private final CustomerService customerService;

	public TicketService(CustomerService customerService) {
		this.customerService = customerService;
	}

	@PostConstruct
	void seedData() {
		LocalDateTime now = LocalDateTime.now();
		tickets.put("TCK-1045", buildSeed("TCK-1045", "CLI-001", "Geladeira Brastemp BWA12AB",
				"Não gela o suficiente mesmo após limpeza recente.", TicketStatus.EM_ANDAMENTO,
				now.minusDays(2).format(DATE_FORMAT)));
		tickets.put("TCK-1046", buildSeed("TCK-1046", "CLI-002", "Televisão Samsung 55\"",
				"Televisor liga, porém não exibe imagem.", TicketStatus.EM_ANDAMENTO,
				now.minusDays(1).format(DATE_FORMAT)));
		tickets.put("TCK-1047", buildSeed("TCK-1047", "CLI-003", "Máquina de Lavar Consul MWK12AB",
				"Apresenta ruído e não completa centrifugação.", TicketStatus.TRIAGEM,
				now.minusHours(5).format(DATE_FORMAT)));
		tickets.put("TCK-1048", buildSeed("TCK-1048", "CLI-004", "Micro-ondas Electrolux MTD30",
				"Painel liga mas não aquece alimentos.", TicketStatus.EM_ANDAMENTO,
				now.minusHours(3).format(DATE_FORMAT)));
		counter.set(1048);
	}

	private Ticket buildSeed(String id, String customerId, String device, String description, TicketStatus status,
			String entryDate) {
		Customer customer = customerService.getRequired(customerId);
		return new Ticket(id, customerId, customer.name(), device, description, status, entryDate);
	}

	public Ticket create(String customerId, String deviceType, String deviceModel, String description) {
		Customer customer = customerService.getRequired(customerId);
		String ticketId = "TCK-" + counter.incrementAndGet();
		String entryDate = LocalDateTime.now().format(DATE_FORMAT);
		String device = (deviceType + " " + deviceModel).trim();

		var ticket = new Ticket(ticketId, customerId, customer.name(), device, description.trim(),
				TicketStatus.TRIAGEM, entryDate);
		tickets.put(ticketId, ticket);
		return ticket;
	}

	public List<Ticket> findAll() {
		return new ArrayList<>(tickets.values());
	}

	public Optional<Ticket> findById(String id) {
		return Optional.ofNullable(tickets.get(id));
	}

	public Ticket getRequired(String id) {
		return findById(id).orElseThrow(() -> new IllegalArgumentException("Chamado %s não encontrado".formatted(id)));
	}

	public record Ticket(String id, String customerId, String customerName, String device, String description,
			TicketStatus status, String entryDate) {
	}

	public enum TicketStatus {
		TRIAGEM, EM_ANDAMENTO, FINALIZADO
	}
}


