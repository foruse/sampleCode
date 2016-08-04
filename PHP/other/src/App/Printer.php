<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace App;

/**
 * Class Printer
 */
class Printer implements PrinterInterface
{
    /**
     * @var string
     */
    private $text;

    /**
     * Printer constructor.
     *
     * @param string $text
     */
    public function __construct($text)
    {
        $this->text = $text;
    }

    /**
     * @inheritdoc
     */
    public function printText()
    {
        echo $this->text;
    }
}
