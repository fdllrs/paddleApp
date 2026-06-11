-- Extend the ticket_status enum with PROCESSING and FAILED states
ALTER TYPE ticket_status ADD VALUE 'PROCESSING';
ALTER TYPE ticket_status ADD VALUE 'FAILED';
