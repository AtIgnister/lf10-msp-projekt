import 'charts.css'
function copy()
{
    let selection = document.getElementById("emotion");
    let comment = document.getElementById("comment");
    navigator.clipboard.writeText("Emotion: " + selection.value + '; Comment: ' + comment.value + ';');
}