import 'jspdf';
import type { Table } from 'jspdf-autotable';
/** Property installed on the document by the bundled AutoTable plugin. */
declare module 'jspdf' {
  interface jsPDF { lastAutoTable?: Table }
}
