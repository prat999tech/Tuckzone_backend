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
