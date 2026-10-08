// 打印/导出依赖装载 —— 产物 print-export chunk（html2canvas + jsPDF 的 CJS 打包）等价（npm 替换）
// 产物用法：工厂 br() 返回模块 exports，经 interopDefaultCompat(x,1).default 取到 html2canvas
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';

export function loadPrintExport() {
  return html2canvas;
}

export function loadJsPdf() {
  return jsPDF;
}
