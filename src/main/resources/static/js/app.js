function addRow(className) {
  const template = document.querySelector('.' + className);
  if (!template) return;
  const copy = template.cloneNode(true);
  copy.querySelectorAll('input').forEach(input => {if (input.type === 'number') input.value = input.name === 'cost' ? '' : '1'; else input.value = '';});
  copy.querySelectorAll('select').forEach(select => select.selectedIndex = 0);
  document.querySelector('#lines').appendChild(copy);
}
function removeRow(button) {
  const group = button.closest('.line');
  if (!group || group.parentElement.children.length === 1) return;
  group.remove();
}
