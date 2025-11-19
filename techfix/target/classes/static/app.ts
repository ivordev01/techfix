type Ticket = {
  id: string;
  customer: string;
  device: string;
  status: "TRIAGEM" | "EM_ANDAMENTO" | "FINALIZADO";
  etaMinutes: number;
  description?: string;
};

type DashboardSummary = {
  pendingTickets: number;
  techniciansAvailable: number;
  nextAppointment: string;
  todayTickets: number;
  tickets: Ticket[];
};

const $ = <T extends HTMLElement>(selector: string): T => {
  const element = document.querySelector(selector);
  if (!element) {
    throw new Error(`Elemento ${selector} não encontrado`);
  }
  return element as T;
};

const pipeline = $("#ticket-pipeline");
const form = $("#ticket-form") as HTMLFormElement;
const ctaButton = $("#cta-btn");
const pill = $("#pipeline-pill");

const setMetricText = (selector: string, value: string): void => {
  $(selector).textContent = value;
};

const statusLabel: Record<Ticket["status"], string> = {
  TRIAGEM: "Em triagem",
  EM_ANDAMENTO: "Em andamento",
  FINALIZADO: "Concluído",
};

let currentTickets: Ticket[] = [];

const renderTickets = (tickets: Ticket[]): void => {
  currentTickets = tickets;
  pipeline.innerHTML = "";

  if (!tickets.length) {
    pipeline.innerHTML = '<p class="placeholder">Nenhum chamado no momento.</p>';
    return;
  }

  tickets.forEach((ticket) => {
    const card = document.createElement("article");
    card.className = "ticket-card";
    card.innerHTML = `
      <div>
        <strong>${ticket.customer}</strong>
        <span>${ticket.device}</span>
      </div>
      <div>
        <span>${statusLabel[ticket.status]}</span>
        <strong>${ticket.etaMinutes} min</strong>
      </div>
    `;
    pipeline.appendChild(card);
  });
};

const handleError = (message: string): void => {
  pill.textContent = "erro";
  pill.classList.add("pill--error");
  pipeline.innerHTML = `<p class="placeholder">${message}</p>`;
};

const loadDashboard = async (): Promise<void> => {
  pill.textContent = "sincronizando…";
  pill.classList.remove("pill--error");

  try {
    const response = await fetch("/api/dashboard");
    if (!response.ok) {
      throw new Error("Falha ao carregar o painel");
    }

    const summary = (await response.json()) as DashboardSummary;
    setMetricText("#metric-today", summary.todayTickets.toString());
    setMetricText("#metric-techs", summary.techniciansAvailable.toString());
    setMetricText("#metric-next", summary.nextAppointment);
    pill.textContent = `${summary.pendingTickets} em aberto`;

    renderTickets(summary.tickets);
  } catch (error) {
    console.error(error);
    handleError("Não foi possível sincronizar. Tente novamente.");
  }
};

const showToast = (message: string): void => {
  const toast = document.createElement("div");
  toast.textContent = message;
  toast.style.position = "fixed";
  toast.style.bottom = "2rem";
  toast.style.right = "2rem";
  toast.style.background = "#0f172a";
  toast.style.color = "white";
  toast.style.padding = "0.75rem 1.25rem";
  toast.style.borderRadius = "999px";
  toast.style.boxShadow = "0 15px 35px rgba(15, 23, 42, 0.35)";
  toast.style.zIndex = "100";
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2500);
};

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const formData = new FormData(form);
  const ticket: Ticket = {
    id: crypto.randomUUID(),
    customer: formData.get("customer") as string,
    device: formData.get("device") as string,
    description: formData.get("description") as string,
    status: "TRIAGEM",
    etaMinutes: 45,
  };

  renderTickets([ticket, ...currentTickets]);
  form.reset();
  showToast("Chamado registrado (mock)");
});

ctaButton.addEventListener("click", () => {
  form.scrollIntoView({ behavior: "smooth" });
  (form.elements.namedItem("customer") as HTMLInputElement)?.focus();
});

loadDashboard();
