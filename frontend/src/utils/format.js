/**
 * Collapses the full order lifecycle (PLACED/ACCEPTED/PREPARING/PACKED/OUT_FOR_DELIVERY/
 * REJECTED/CANCELLED/DELIVERED) down to the two words customers and admins actually see.
 * The real status still drives every business rule (Accept/Reject, filters, action
 * buttons) — this only simplifies what gets printed on screen.
 */
export function orderStatusLabel(status) {
  return status === 'DELIVERED' ? 'Delivered' : 'Placed';
}

/** "VIII-B" style class label, used wherever a student's class/section needs to render as
 *  one value instead of two separate fields side by side. */
export function classLabel(studentClass, section) {
  if (!studentClass) return '';
  return section ? `${studentClass}-${section}` : studentClass;
}

/** Short, readable date (e.g. "9 Aug 2026"), matching mobile's formatDate exactly so a
 *  date reads the same wherever a user might see both apps. Accepts an ISO date/datetime
 *  string (a plain YYYY-MM-DD parses as UTC midnight, which is fine for en-IN display). */
export function formatDate(iso) {
  const date = new Date(iso);
  return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}
